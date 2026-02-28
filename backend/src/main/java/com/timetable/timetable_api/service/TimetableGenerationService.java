package com.timetable.timetable_api.service;

import com.timetable.timetable_api.model.*;
import com.timetable.timetable_api.repository.*;
import com.timetable.timetable_api.dto.UpdateSlotRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TimetableGenerationService {

    @Autowired
    private CourseOfferingRepository offeringRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private ScheduledClassRepository scheduledClassRepository;

    /**
     * Advanced deterministic timetable generation with strict constraints.
     * Phases:
     * A) Setup: clear old classes, prepare data and trackers
     * B) Prioritized loop (LAB first, then THEORY by descending creditHours)
     * C) Constraints: room-type, capacity, resource conflicts, faculty limits,
     * block integrity
     * D) Post-processing and failure reporting
     */
    @Transactional
    public List<ScheduledClass> generateTimetable() {
        // Phase A: Setup and data initialization
        scheduledClassRepository.deleteAll();

        List<CourseOffering> allOfferings = offeringRepository.findAll();
        if (allOfferings.isEmpty()) {
            return List.of();
        }

        List<Room> allRooms = roomRepository.findAll();
        if (allRooms.isEmpty()) {
            throw new RuntimeException("No rooms configured. Please add rooms before generating a timetable.");
        }

        // Get all non-break timeslots
        List<TimeSlot> allTimeSlots = timeSlotRepository.findAllByOrderByDayOfWeekAscStartTimeAsc()
                .stream()
                .filter(ts -> !ts.isBreakSlot())
                .sorted(Comparator
                        .comparing((TimeSlot ts) -> dayOrder(ts.getDayOfWeek()))
                        .thenComparing(TimeSlot::getStartTime))
                .collect(Collectors.toList());
        if (allTimeSlots.isEmpty()) {
            throw new RuntimeException(
                    "No time slots configured. Please create time slots before generating a timetable.");
        }

        Map<String, List<TimeSlot>> slotsByDay = allTimeSlots.stream()
                .collect(Collectors.groupingBy(ts -> normalizeDay(ts.getDayOfWeek()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        // Workload tracking
        Map<Long, Integer> facultyCreditHours = new HashMap<>(); // ID -> hours
        Map<String, Integer> sectionDailyHours = new HashMap<>(); // "SectionID_DAY" -> hours
        Set<String> occupied = new HashSet<>(); // Keys for collision detection
        List<ScheduledClass> created = new ArrayList<>();

        // Phase B: Strategies by Semester
        Map<Integer, List<CourseOffering>> bySemester = allOfferings.stream()
                .collect(Collectors.groupingBy(o -> o.getSection().getSemester()));

        for (Integer semester : bySemester.keySet()) {
            List<CourseOffering> semesterOfferings = bySemester.get(semester);

            // Sort: Labs/Practicals high priority, then by total credit hours
            semesterOfferings.sort((a, b) -> {
                int pA = a.getCourse().getPracticalHours();
                int pB = b.getCourse().getPracticalHours();
                if (pA != pB)
                    return Integer.compare(pB, pA); // Higher practicals first
                return Integer.compare(
                        b.getCourse().getCreditHours(),
                        a.getCourse().getCreditHours());
            });

            for (CourseOffering offering : semesterOfferings) {
                Course c = offering.getCourse();
                if (c == null)
                    continue;

                // 1. Schedule Practicals (Block of 2 or specific hours)
                int pHours = Optional.ofNullable(c.getPracticalHours()).orElse(0);
                if (pHours > 0) {
                    boolean scheduled = scheduleComponent(offering, "PRACTICAL", pHours, 2, allRooms, slotsByDay,
                            occupied, facultyCreditHours, sectionDailyHours, created);
                    if (!scheduled)
                        throw new RuntimeException("Failed to schedule Practical for " + c.getCourseCode());
                }

                // 2. Schedule Lectures (Block of 1)
                int lHours = Optional.ofNullable(c.getLectureHours()).orElse(0);
                if (lHours > 0) {
                    // Try to assume creditHours if L/T/P are all 0 (legacy support)
                    if (lHours == 0 && pHours == 0 && c.getTutorialHours() == 0) {
                        lHours = c.getCreditHours();
                    }
                    boolean scheduled = scheduleComponent(offering, "LECTURE", lHours, 1, allRooms, slotsByDay,
                            occupied, facultyCreditHours, sectionDailyHours, created);
                    if (!scheduled)
                        throw new RuntimeException("Failed to schedule Lecture for " + c.getCourseCode());
                }

                // 3. Schedule Tutorials (Block of 1)
                int tHours = Optional.ofNullable(c.getTutorialHours()).orElse(0);
                if (tHours > 0) {
                    boolean scheduled = scheduleComponent(offering, "TUTORIAL", tHours, 1, allRooms, slotsByDay,
                            occupied, facultyCreditHours, sectionDailyHours, created);
                    if (!scheduled)
                        throw new RuntimeException("Failed to schedule Tutorial for " + c.getCourseCode());
                }
            }
        }

        return created;
    }

    private boolean scheduleComponent(
            CourseOffering offering,
            String type,
            int totalHours,
            int blockSize,
            List<Room> allRooms,
            Map<String, List<TimeSlot>> slotsByDay,
            Set<String> occupied,
            Map<Long, Integer> facultyCreditHours,
            Map<String, Integer> sectionDailyHours,
            List<ScheduledClass> created) {
        int scheduledCount = 0;
        int neededBlocks = (int) Math.ceil((double) totalHours / blockSize);

        List<String> days = new ArrayList<>(orderedDays());

        for (int b = 0; b < neededBlocks; b++) {
            boolean placed = false;

            // Sort days by minimum hours assigned for this section (US-20: uniform
            // distribution)
            List<String> sortedDays = new ArrayList<>(days);
            sortedDays.sort(Comparator
                    .comparingInt(d -> sectionDailyHours.getOrDefault(offering.getSection().getId() + "_" + d, 0)));

            for (String day : sortedDays) {
                List<TimeSlot> daySlots = slotsByDay.getOrDefault(day, List.of());

                // Try slots
                for (int i = 0; i <= daySlots.size() - blockSize; i++) {
                    List<TimeSlot> block = daySlots.subList(i, i + blockSize);
                    if (!isContiguous(block))
                        continue;

                    // US-22: Skip lunch and short breaks based on semester
                    if (isBreakForSemester(block, offering.getSection().getSemester())) {
                        continue;
                    }

                    // US-21: Block Saturday slots after lunch for club activities
                    if (isSaturdayAfterLunch(block)) {
                        continue;
                    }

                    // US-19: Strict check for consecutive faculty classes (gap of 55 minutes)
                    if (hasConsecutiveFacultyClass(offering.getFaculty(), day, block, occupied, daySlots)) {
                        continue;
                    }

                    // Try Rooms
                    for (Room room : allRooms) {
                        if (!roomTypeMatches(room, type))
                            continue;
                        if (!hasCapacity(room, offering.getSection()))
                            continue;

                        if (checkResourcesFree(offering, room, block, occupied, facultyCreditHours, blockSize)) {
                            // Book it
                            for (TimeSlot slot : block) {
                                created.add(save(offering, room, slot));
                                occupy(offering, room, slot, occupied);
                            }
                            incrementHours(offering.getFaculty(), facultyCreditHours, blockSize);
                            sectionDailyHours.merge(offering.getSection().getId() + "_" + day, blockSize, Integer::sum);
                            placed = true;
                            break;
                        }
                    }
                    if (placed)
                        break;
                }
                if (placed) {
                    break;
                }
            }
            if (placed)
                scheduledCount++;
        }

        return scheduledCount == neededBlocks;
    }

    private boolean hasConsecutiveFacultyClass(Faculty faculty, String day, List<TimeSlot> block, Set<String> occupied,
            List<TimeSlot> daySlots) {
        long fid = faculty.getId();

        TimeSlot firstSlot = block.get(0);
        TimeSlot lastSlot = block.get(block.size() - 1);

        int firstIdx = daySlots.indexOf(firstSlot);
        int lastIdx = daySlots.indexOf(lastSlot);

        if (firstIdx > 0) {
            TimeSlot prev = daySlots.get(firstIdx - 1);
            if (occupied.contains(key("FACULTY", fid, day, prev.getStartTime())))
                return true;
        }

        if (lastIdx >= 0 && lastIdx < daySlots.size() - 1) {
            TimeSlot next = daySlots.get(lastIdx + 1);
            if (occupied.contains(key("FACULTY", fid, day, next.getStartTime())))
                return true;
        }

        return false;
    }

    private boolean roomTypeMatches(Room room, String type) {
        String rType = room.getType().trim().toUpperCase();
        if ("PRACTICAL".equals(type) || "LAB".equals(type))
            return "LAB".equalsIgnoreCase(rType);
        return "CLASSROOM".equalsIgnoreCase(rType) || "THEORY".equalsIgnoreCase(rType);
    }

    /**
     * Update a scheduled class slot with conflict checks. Transactional.
     */
    @Transactional
    public ScheduledClass updateScheduledClassSlot(com.timetable.timetable_api.dto.UpdateSlotRequest req) {
        if (req.getScheduledClassId() == null || req.getNewRoomId() == null
                || req.getNewDayOfWeek() == null || req.getNewStartTime() == null) {
            throw new RuntimeException("Missing required fields in request");
        }

        ScheduledClass sc = scheduledClassRepository.findById(req.getScheduledClassId())
                .orElseThrow(() -> new RuntimeException("Scheduled class not found: " + req.getScheduledClassId()));

        Room room = roomRepository.findById(req.getNewRoomId())
                .orElseThrow(() -> new RuntimeException("Room not found: " + req.getNewRoomId()));

        String day = req.getNewDayOfWeek().trim().toUpperCase();

        // Resolve end time by matching the TimeSlot record with new day+start
        TimeSlot newSlot = timeSlotRepository.findAllByOrderByDayOfWeekAscStartTimeAsc().stream()
                .filter(ts -> !ts.isBreakSlot())
                .filter(ts -> day.equals(ts.getDayOfWeek().trim().toUpperCase()))
                .filter(ts -> req.getNewStartTime().equals(ts.getStartTime()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Invalid time slot: " + day + " " + req.getNewStartTime()));

        CourseOffering off = sc.getCourseOffering();
        if (off == null || off.getCourse() == null || off.getSection() == null || off.getFaculty() == null) {
            throw new RuntimeException("Scheduled class has invalid offering links");
        }

        // Physical constraints
        if (!roomTypeMatches(room, off.getCourse())) {
            throw new RuntimeException("Conflict: Room type incompatible with course type");
        }
        if (!hasCapacity(room, off.getSection())) {
            throw new RuntimeException("Conflict: Room capacity insufficient for section");
        }

        // Resource conflicts at the requested time
        // Room
        boolean roomBusy = scheduledClassRepository
                .findByRoom_IdAndDayOfWeekAndStartTime(room.getId(), day, req.getNewStartTime())
                .stream()
                .anyMatch(other -> !Objects.equals(other.getId(), sc.getId()));
        if (roomBusy) {
            throw new RuntimeException("Conflict: Room is already occupied at that time");
        }

        // Faculty/Section conflicts: any class for the same faculty/section at the same
        // time
        List<ScheduledClass> sameTime = scheduledClassRepository.findByDayOfWeekAndStartTime(day,
                req.getNewStartTime());
        boolean facultyBusy = sameTime.stream()
                .anyMatch(other -> !Objects.equals(other.getId(), sc.getId())
                        && other.getCourseOffering() != null
                        && other.getCourseOffering().getFaculty() != null
                        && Objects.equals(other.getCourseOffering().getFaculty().getId(), off.getFaculty().getId()));
        if (facultyBusy) {
            throw new RuntimeException("Conflict: Faculty is already scheduled at that time");
        }
        boolean sectionBusy = sameTime.stream()
                .anyMatch(other -> !Objects.equals(other.getId(), sc.getId())
                        && other.getCourseOffering() != null
                        && other.getCourseOffering().getSection() != null
                        && Objects.equals(other.getCourseOffering().getSection().getId(), off.getSection().getId()));
        if (sectionBusy) {
            throw new RuntimeException("Conflict: Section is already scheduled at that time");
        }

        // If all good, apply update
        sc.setRoom(room);
        sc.setDayOfWeek(day);
        sc.setStartTime(req.getNewStartTime());
        sc.setEndTime(newSlot.getEndTime());
        return scheduledClassRepository.save(sc);
    }

    // ---------------- helper methods ----------------

    private boolean isContiguous(List<TimeSlot> block) {
        if (block.size() < 2)
            return true;
        String day = normalizeDay(block.get(0).getDayOfWeek());
        for (int i = 0; i < block.size() - 1; i++) {
            TimeSlot a = block.get(i);
            TimeSlot b = block.get(i + 1);
            if (a.isBreakSlot() || b.isBreakSlot())
                return false;
            if (!normalizeDay(a.getDayOfWeek()).equals(day))
                return false;
            LocalTime ae = a.getEndTime();
            LocalTime bs = b.getStartTime();
            if (ae == null || bs == null || !ae.equals(bs))
                return false;
        }
        return true;
    }

    private boolean isBreakForSemester(List<TimeSlot> block, Integer semester) {
        if (semester == null)
            return false;
        for (TimeSlot slot : block) {
            LocalTime start = slot.getStartTime();
            LocalTime end = slot.getEndTime();

            // Sem 3,4 breaks: 10:50-11:10 and 13:00-13:55
            if (semester == 3 || semester == 4) {
                if (isOverlapping(start, end, LocalTime.of(10, 50), LocalTime.of(11, 10)))
                    return true;
                if (isOverlapping(start, end, LocalTime.of(13, 00), LocalTime.of(13, 55)))
                    return true;
            }

            // Sem 5,6,7 breaks: 09:55-10:15 and 12:05-13:00
            if (semester >= 5 && semester <= 7) {
                if (isOverlapping(start, end, LocalTime.of(9, 55), LocalTime.of(10, 15)))
                    return true;
                if (isOverlapping(start, end, LocalTime.of(12, 05), LocalTime.of(13, 00)))
                    return true;
            }

            // Generic lunch break fallback
            if (semester < 3 || semester > 7) {
                if (isOverlapping(start, end, LocalTime.of(13, 00), LocalTime.of(13, 55)))
                    return true;
            }
        }
        return false;
    }

    private boolean isOverlapping(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    private boolean isSaturdayAfterLunch(List<TimeSlot> block) {
        for (TimeSlot slot : block) {
            if ("SATURDAY".equalsIgnoreCase(normalizeDay(slot.getDayOfWeek()))) {
                if (!slot.getStartTime().isBefore(LocalTime.of(13, 0))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean roomTypeMatches(Room room, Course course) {
        String rtype = room.getType() == null ? "" : room.getType().trim().toUpperCase();
        String ctype = courseType(course);
        if (isLab(ctype)) {
            return "LAB".equalsIgnoreCase(rtype);
        }
        // THEORY must be in a CLASSROOM (allow synonyms THEORY/CLASSROOM)
        return "CLASSROOM".equalsIgnoreCase(rtype) || "THEORY".equalsIgnoreCase(rtype);
    }

    private boolean hasCapacity(Room room, Section section) {
        Integer capacity = room.getCapacity();
        Integer studentCount = section.getStudentCount();
        int required = studentCount == null ? 30 : studentCount; // sensible default
        return capacity != null && capacity >= required;
    }

    private boolean checkResourcesFree(
            CourseOffering offering,
            Room room,
            List<TimeSlot> slots,
            Set<String> occupied,
            Map<Long, Integer> facultyHours,
            int hoursToAdd) {
        Faculty faculty = offering.getFaculty();
        Section section = offering.getSection();

        // Faculty workload check
        int current = facultyHours.getOrDefault(faculty.getId(), 0);
        int limit = getFacultyMaxHours(faculty);
        if (current + hoursToAdd > limit)
            return false;

        // Resource availability across all slots in the block
        for (TimeSlot slot : slots) {
            String day = normalizeDay(slot.getDayOfWeek());
            String roomKey = key("ROOM", room.getId(), day, slot.getStartTime());
            String facultyKey = key("FACULTY", faculty.getId(), day, slot.getStartTime());
            String sectionKey = key("SECTION", section.getId(), day, slot.getStartTime());
            if (occupied.contains(roomKey) || occupied.contains(facultyKey) || occupied.contains(sectionKey)) {
                return false;
            }
        }
        return true;
    }

    private void occupy(CourseOffering offering, Room room, TimeSlot slot, Set<String> occupied) {
        String day = normalizeDay(slot.getDayOfWeek());
        occupied.add(key("ROOM", room.getId(), day, slot.getStartTime()));
        occupied.add(key("FACULTY", offering.getFaculty().getId(), day, slot.getStartTime()));
        occupied.add(key("SECTION", offering.getSection().getId(), day, slot.getStartTime()));
    }

    private void incrementHours(Faculty faculty, Map<Long, Integer> facultyHours, int inc) {
        facultyHours.merge(faculty.getId(), inc, Integer::sum);
    }

    private ScheduledClass save(CourseOffering offering, Room room, TimeSlot slot) {
        ScheduledClass sc = new ScheduledClass();
        sc.setCourseOffering(offering);
        sc.setRoom(room);
        sc.setDayOfWeek(normalizeDay(slot.getDayOfWeek()));
        sc.setStartTime(slot.getStartTime());
        sc.setEndTime(slot.getEndTime());
        return scheduledClassRepository.save(sc);
    }

    private int getFacultyMaxHours(Faculty faculty) {
        DesignationConstraint dc = faculty.getDesignationConstraint();
        if (dc == null)
            return Integer.MAX_VALUE;
        int lec = Optional.ofNullable(dc.getMaxLectureHours()).orElse(0);
        int lab = Optional.ofNullable(dc.getMaxLabHours()).orElse(0);
        // Use combined limit to avoid under-counting mixed loads
        return lec + lab;
    }

    private String courseType(Course course) {
        String t = course == null ? null : course.getCourseType();
        return (t == null || t.isBlank()) ? "THEORY" : t.trim().toUpperCase();
    }

    private boolean isLab(String type) {
        return "LAB".equalsIgnoreCase(type);
    }

    private String key(String resource, Number id, String day, LocalTime start) {
        return resource + "_" + id + "_" + day + "_" + (start == null ? "" : start.toString());
    }

    private List<String> orderedDays() {
        return List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");
    }

    private String normalizeDay(String day) {
        if (day == null)
            return "";
        return day.trim().toUpperCase();
    }

    private int dayOrder(String day) {
        if (day == null)
            return 8;
        try {
            return DayOfWeek.valueOf(day.trim().toUpperCase()).getValue();
        } catch (IllegalArgumentException ex) {
            return 8;
        }
    }
}
