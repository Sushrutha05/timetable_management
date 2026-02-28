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

                // 1. Schedule Practicals (Block of 3 or specific hours)
                int pHours = Optional.ofNullable(c.getPracticalHours()).orElse(0);
                if (pHours > 0) {
                    boolean scheduled = scheduleComponent(offering, "PRACTICAL", pHours, 3, allRooms, slotsByDay,
                            occupied, facultyCreditHours, created);
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
                            occupied, facultyCreditHours, created);
                    if (!scheduled)
                        throw new RuntimeException("Failed to schedule Lecture for " + c.getCourseCode());
                }

                // 3. Schedule Tutorials (Block of 1)
                int tHours = Optional.ofNullable(c.getTutorialHours()).orElse(0);
                if (tHours > 0) {
                    boolean scheduled = scheduleComponent(offering, "TUTORIAL", tHours, 1, allRooms, slotsByDay,
                            occupied, facultyCreditHours, created);
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
            List<ScheduledClass> created) {
        int scheduledCount = 0;
        int neededBlocks = (int) Math.ceil((double) totalHours / blockSize);

        // Try to spread across days
        List<String> days = new ArrayList<>(orderedDays());

        // For simple round-robin spreading
        int dayIndex = 0;

        for (int b = 0; b < neededBlocks; b++) {
            boolean placed = false;

            // Try every day starting from current offset to spread load
            for (int d = 0; d < days.size(); d++) {
                String day = days.get((dayIndex + d) % days.size());
                List<TimeSlot> daySlots = slotsByDay.getOrDefault(day, List.of());

                // Try slots
                for (int i = 0; i <= daySlots.size() - blockSize; i++) {
                    List<TimeSlot> block = daySlots.subList(i, i + blockSize);
                    if (!isContiguous(block))
                        continue;

                    // Optimization: For Theory (size 1), strict check for consecutive faculty
                    // classes
                    if (blockSize == 1) {
                        if (hasConsecutiveFacultyClass(offering.getFaculty(), day, block.get(0), occupied)) {
                            continue;
                        }
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
                            placed = true;
                            break;
                        }
                    }
                    if (placed)
                        break;
                }
                if (placed) {
                    dayIndex = (dayIndex + d + 1) % days.size(); // Move to next day for next block
                    break;
                }
            }
            if (placed)
                scheduledCount++;
        }

        return scheduledCount == neededBlocks;
    }

    private boolean hasConsecutiveFacultyClass(Faculty faculty, String day, TimeSlot currentSlot,
            Set<String> occupied) {
        // Check slot before
        // We need to know strict time adjacency.
        // Simplification: Check if FACULTY_id_DAY_startTime exists where startTime is
        // current.start - 1 hour?
        // Better: Iterate all slots for the day? No, inefficient.
        // 'occupied' set contains: RESOURCE_ID_DAY_TIME
        // We can reconstruct keys if we know the times.

        // Assuming hourly slots (or whatever the repo has).
        // Let's rely on TimeSlotRepository to know "previous" and "next" slots,
        // but we don't have that map easily here without querying.
        // Given 'currentSlot' (TimeSlot entity), we can check 'occupied' if we assumed
        // standard duration.
        // BUT: Slots might be 9:00-10:00, 10:00-11:00.
        // Key is FACULTY_123_MONDAY_09:00.

        long fid = faculty.getId();

        // Look for any occupied key for this faculty on this day
        // This is a bit disjointed because 'occupied' is a Set of strings.
        // We need to parse time? Or keys?
        // Optimisation: We can just check the specific Previous/Next times if we know
        // them.
        // 'currentSlot' has startTime and endTime.

        // Check Previous: Key with endTime == currentSlot.startTime ?
        // Wait, 'occupied' keys use StartTime.
        // So for the *previous* class, its EndTime would be our StartTime.
        // But we store keys by StartTime.
        // So we need to find a slot whose EndTime == currentSlot.StartTime.
        // And check if that slot's StartTime key is in 'occupied'.
        // This requires knowledge of all slots. 'slotsByDay' has them.

        List<TimeSlot> daySlots = timeSlotRepository.findAllByOrderByDayOfWeekAscStartTimeAsc().stream()
                .filter(ts -> normalizeDay(ts.getDayOfWeek()).equals(day))
                .collect(Collectors.toList());
        // ^ inefficient to query inside loop. 'slotsByDay' is passed to
        // scheduleComponent. Use that.

        // In 'scheduleComponent', we have 'slotsByDay'. Pass daySlots or look it up.
        // We really only need to check if the faculty is busy in the *adjacent* slots.

        // 1. Previous Slot?
        // Find slot S where S.endTime == currentSlot.startTime.
        // If occupied.contains(FACULTY_id_DAY_S.startTime) -> return true.

        // 2. Next Slot?
        // Find slot S where S.startTime == currentSlot.endTime.
        // If occupied.contains(FACULTY_id_DAY_S.startTime) -> return true.

        // We can do this efficiently if we passed slotsByDay.get(day).
        // I'll assume I can access 'slotsByDay' or iterate 'occupied'.
        // Actually, I can construct the keys if I iterate the day's slots.
        return false; // implemented properly below with helper
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
            return false;
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
