package com.timetable.timetable_api.service;

import com.timetable.timetable_api.model.*;
import com.timetable.timetable_api.repository.*;
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
     *  A) Setup: clear old classes, prepare data and trackers
     *  B) Prioritized loop (LAB first, then THEORY by descending creditHours)
     *  C) Constraints: room-type, capacity, resource conflicts, faculty limits, block integrity
     *  D) Post-processing and failure reporting
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

        // Get all non-break timeslots, sorted by day and time
        List<TimeSlot> allTimeSlots = timeSlotRepository.findAllByOrderByDayOfWeekAscStartTimeAsc()
                .stream()
                .filter(ts -> !ts.isBreakSlot())
                .sorted(Comparator
                        .comparing((TimeSlot ts) -> dayOrder(ts.getDayOfWeek()))
                        .thenComparing(TimeSlot::getStartTime))
                .collect(Collectors.toList());
        if (allTimeSlots.isEmpty()) {
            throw new RuntimeException("No time slots configured. Please create time slots before generating a timetable.");
        }

        // Index time slots by day for faster block checks
        Map<String, List<TimeSlot>> slotsByDay = allTimeSlots.stream()
                .collect(Collectors.groupingBy(ts -> normalizeDay(ts.getDayOfWeek()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        // Workload tracking: facultyId -> current scheduled credit hours
        Map<Long, Integer> facultyCreditHours = new HashMap<>();

        // Conflict tracking: resource occupancy keys
        Set<String> occupied = new HashSet<>();

        // Phase B: Prioritized scheduling list (LAB first, then THEORY high to low by credit hours)
        List<CourseOffering> labsFirst = allOfferings.stream()
                .sorted((a, b) -> {
                    String ta = courseType(a.getCourse());
                    String tb = courseType(b.getCourse());
                    if (!ta.equals(tb)) {
                        // LAB before THEORY
                        if (isLab(ta)) return -1;
                        if (isLab(tb)) return 1;
                    }
                    // For same type (usually THEORY), schedule higher credit hours first
                    int ca = Optional.ofNullable(a.getCourse().getCreditHours()).orElse(1);
                    int cb = Optional.ofNullable(b.getCourse().getCreditHours()).orElse(1);
                    return Integer.compare(cb, ca);
                })
                .collect(Collectors.toList());

        List<ScheduledClass> created = new ArrayList<>();

        for (CourseOffering offering : labsFirst) {
            Course course = offering.getCourse();
            Section section = offering.getSection();
            Faculty faculty = offering.getFaculty();

            if (course == null || section == null || faculty == null) {
                throw new RuntimeException("Invalid offering links (course/faculty/section missing) for offering id=" + offering.getId());
            }

            boolean isLab = isLab(courseType(course));
            int slotsNeeded = isLab ? 3 : Math.max(1, Optional.ofNullable(course.getCreditHours()).orElse(1));

            boolean placed = false;

            // Deterministic day ordering (MON .. SUN)
            for (String day : orderedDays()) {
                List<TimeSlot> daySlots = slotsByDay.getOrDefault(day, List.of());
                if (daySlots.isEmpty()) continue;

                // LAB: search consecutive block of 3; THEORY: iterate single slots sequentially with possible repetition until slotsNeeded fulfilled
                if (isLab) {
                    // Try each starting index for a contiguous block of 3
                    for (int i = 0; i <= daySlots.size() - 3; i++) {
                        List<TimeSlot> block = daySlots.subList(i, i + 3);
                        if (!isContiguous(block)) continue;

                        // Try each room deterministically
                        for (Room room : allRooms) {
                            if (!roomTypeMatches(room, course)) continue;
                            if (!hasCapacity(room, section)) continue;

                            if (!checkResourcesFree(offering, room, block, occupied, facultyCreditHours, slotsNeeded)) {
                                continue;
                            }

                            // Book block
                            for (TimeSlot slot : block) {
                                created.add(save(offering, room, slot));
                                occupy(offering, room, slot, occupied);
                            }
                            incrementHours(faculty, facultyCreditHours, slotsNeeded);
                            placed = true;
                            break;
                        }
                        if (placed) break;
                    }
                } else {
                    int scheduled = 0;
                    outer:
                    for (TimeSlot slot : daySlots) {
                        for (Room room : allRooms) {
                            if (!roomTypeMatches(room, course)) continue;
                            if (!hasCapacity(room, section)) continue;

                            if (!checkResourcesFree(offering, room, List.of(slot), occupied, facultyCreditHours, 1)) {
                                continue;
                            }

                            created.add(save(offering, room, slot));
                            occupy(offering, room, slot, occupied);
                            incrementHours(faculty, facultyCreditHours, 1);
                            scheduled++;
                            if (scheduled >= slotsNeeded) { placed = true; break outer; }
                            // else continue to pick next available slot on same day (then next days)
                        }
                    }
                }

                if (placed) break; // next offering
            }

            if (!placed) {
                String msg = String.format("Failed to schedule [%s] for section [%s]. Capacity or time slots exhausted.",
                        course.getCourseCode(), section.getName());
                throw new RuntimeException(msg);
            }
        }

        return created;
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

        // Faculty/Section conflicts: any class for the same faculty/section at the same time
        List<ScheduledClass> sameTime = scheduledClassRepository.findByDayOfWeekAndStartTime(day, req.getNewStartTime());
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
        if (block.size() < 2) return false;
        String day = normalizeDay(block.get(0).getDayOfWeek());
        for (int i = 0; i < block.size() - 1; i++) {
            TimeSlot a = block.get(i);
            TimeSlot b = block.get(i + 1);
            if (a.isBreakSlot() || b.isBreakSlot()) return false;
            if (!normalizeDay(a.getDayOfWeek()).equals(day)) return false;
            LocalTime ae = a.getEndTime();
            LocalTime bs = b.getStartTime();
            if (ae == null || bs == null || !ae.equals(bs)) return false;
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
            int hoursToAdd
    ) {
        Faculty faculty = offering.getFaculty();
        Section section = offering.getSection();

        // Faculty workload check
        int current = facultyHours.getOrDefault(faculty.getId(), 0);
        int limit = getFacultyMaxHours(faculty);
        if (current + hoursToAdd > limit) return false;

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
        if (dc == null) return Integer.MAX_VALUE;
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
        if (day == null) return "";
        return day.trim().toUpperCase();
    }

    private int dayOrder(String day) {
        if (day == null) return 8;
        try {
            return DayOfWeek.valueOf(day.trim().toUpperCase()).getValue();
        } catch (IllegalArgumentException ex) {
            return 8;
        }
    }
}
