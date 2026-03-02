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

    @Autowired
    private TimetableMetadataRepository metadataRepository;

    /**
     * Advanced deterministic timetable generation with strict constraints.
     *
     * Key rules observed from real timetables (MITE Sem-4):
     * - Lab sessions are 2–3 contiguous TEACHING slots.
     * - A TEA-BREAK slot may sit inside a lab block (the students are on
     * break; the block still "belongs" to that lab). We therefore allow
     * one break-slot gap when building lab blocks, but we do NOT create
     * a ScheduledClass entry for the break slot itself.
     * - Theory slots must never overlap a break.
     * - Faculty consecutive-class restriction is relaxed for the second and
     * later slots of the same lab block on the same day.
     */
    @Transactional
    public List<ScheduledClass> generateTimetable() {
        // ── Phase A: Setup ──────────────────────────────────────────────────
        scheduledClassRepository.deleteAll();

        TimetableMetadata status = metadataRepository.findByKey("STATUS")
                .orElse(new TimetableMetadata("STATUS", "DRAFT"));
        status.setValue("DRAFT");
        metadataRepository.save(status);

        List<CourseOffering> allOfferings = offeringRepository.findAll();
        if (allOfferings.isEmpty())
            return List.of();

        List<Room> allRooms = roomRepository.findAll();
        if (allRooms.isEmpty())
            throw new RuntimeException("No rooms configured. Please add rooms before generating a timetable.");

        // Fetch ALL slots (including breaks) sorted by day+time.
        // We need breaks for contiguity analysis of lab blocks.
        List<TimeSlot> allSlotsIncludingBreaks = timeSlotRepository.findAllByOrderByDayOfWeekAscStartTimeAsc()
                .stream()
                .sorted(Comparator
                        .comparing((TimeSlot ts) -> dayOrder(ts.getDayOfWeek()))
                        .thenComparing(TimeSlot::getStartTime))
                .collect(Collectors.toList());

        if (allSlotsIncludingBreaks.isEmpty())
            throw new RuntimeException(
                    "No time slots configured. Please create time slots before generating a timetable.");

        // Teaching-only slots (breaks excluded) – used for theory/tutorial
        List<TimeSlot> teachingSlots = allSlotsIncludingBreaks.stream()
                .filter(ts -> !ts.isBreakSlot())
                .collect(Collectors.toList());

        // Group Both sets by day
        Map<String, List<TimeSlot>> teachingByDay = teachingSlots.stream()
                .collect(Collectors.groupingBy(ts -> normalizeDay(ts.getDayOfWeek()),
                        LinkedHashMap::new, Collectors.toList()));
        Map<String, List<TimeSlot>> allByDay = allSlotsIncludingBreaks.stream()
                .collect(Collectors.groupingBy(ts -> normalizeDay(ts.getDayOfWeek()),
                        LinkedHashMap::new, Collectors.toList()));

        // ── Workload trackers ───────────────────────────────────────────────
        Map<Long, Integer> facultyCreditHours = new HashMap<>();
        Map<String, Integer> sectionDailyHours = new HashMap<>();
        Set<String> occupied = new HashSet<>();
        List<ScheduledClass> created = new ArrayList<>();

        // ── Phase B: Schedule by semester ─────────────────────────────────
        Map<Integer, List<CourseOffering>> bySemester = allOfferings.stream()
                .collect(Collectors.groupingBy(o -> o.getSection().getSemester()));

        for (Integer semester : bySemester.keySet()) {
            List<CourseOffering> semesterOfferings = bySemester.get(semester);

            // Labs/Practicals first, then by credit hours descending
            semesterOfferings.sort((a, b) -> {
                int pA = Optional.ofNullable(a.getCourse().getPracticalHours()).orElse(0);
                int pB = Optional.ofNullable(b.getCourse().getPracticalHours()).orElse(0);
                if (pA != pB)
                    return Integer.compare(pB, pA);
                return Integer.compare(
                        Optional.ofNullable(b.getCourse().getCreditHours()).orElse(0),
                        Optional.ofNullable(a.getCourse().getCreditHours()).orElse(0));
            });

            for (CourseOffering offering : semesterOfferings) {
                Course c = offering.getCourse();
                if (c == null)
                    continue;

                int pHours = Optional.ofNullable(c.getPracticalHours()).orElse(0);
                int lHours = Optional.ofNullable(c.getLectureHours()).orElse(0);
                int tHours = Optional.ofNullable(c.getTutorialHours()).orElse(0);

                // Fall-back: if no L/T/P breakdown, treat credit hours as lectures
                if (lHours == 0 && tHours == 0 && pHours == 0) {
                    lHours = Optional.ofNullable(c.getCreditHours()).orElse(0);
                }

                // 1. Practicals — use FULL slot list (including breaks) so the
                // break that sits inside a lab block is transparent.
                if (pHours > 0) {
                    int blockSize = labBlockSize(c);
                    boolean scheduled = schedulePractical(
                            offering, pHours, blockSize,
                            allRooms, allByDay,
                            occupied, facultyCreditHours, sectionDailyHours, created);
                    if (!scheduled)
                        throw new RuntimeException(
                                "Failed to schedule Practical for " + c.getCourseCode()
                                        + " — not enough contiguous lab slots available.");
                }

                // 2. Lectures (block of 1, teaching slots only)
                if (lHours > 0) {
                    boolean scheduled = scheduleComponent(
                            offering, "LECTURE", lHours, 1,
                            allRooms, teachingByDay,
                            occupied, facultyCreditHours, sectionDailyHours, created);
                    if (!scheduled)
                        throw new RuntimeException(
                                "Failed to schedule Lecture for " + c.getCourseCode());
                }

                // 3. Tutorials (block of 1, teaching slots only)
                if (tHours > 0) {
                    boolean scheduled = scheduleComponent(
                            offering, "TUTORIAL", tHours, 1,
                            allRooms, teachingByDay,
                            occupied, facultyCreditHours, sectionDailyHours, created);
                    if (!scheduled)
                        throw new RuntimeException(
                                "Failed to schedule Tutorial for " + c.getCourseCode());
                }
            }
        }

        return created;
    }

    // =========================================================================
    // Lab scheduling — break-spanning aware
    // =========================================================================

    /**
     * Schedule a practical component.
     *
     * A lab block may contain ONE break slot in the middle (tea break).
     * We use the full slot list (including breaks) to build candidate blocks,
     * but skip scheduling (no ScheduledClass) for the break slot itself.
     * The *teaching hours* counted are only the non-break slots in the block.
     */
    private boolean schedulePractical(
            CourseOffering offering,
            int totalPracticalHours,
            int blockSize, // number of TEACHING slots per block
            List<Room> allRooms,
            Map<String, List<TimeSlot>> allByDay,
            Set<String> occupied,
            Map<Long, Integer> facultyCreditHours,
            Map<String, Integer> sectionDailyHours,
            List<ScheduledClass> created) {

        int neededBlocks = (int) Math.ceil((double) totalPracticalHours / blockSize);
        int scheduledCount = 0;

        for (int b = 0; b < neededBlocks; b++) {
            boolean placed = false;

            // Sort days by least-used for this section
            List<String> sortedDays = new ArrayList<>(orderedDays());
            sortedDays.sort(Comparator.comparingInt(
                    d -> sectionDailyHours.getOrDefault(offering.getSection().getId() + "_" + d, 0)));

            outer: for (String day : sortedDays) {
                List<TimeSlot> allDaySlots = allByDay.getOrDefault(day, List.of());

                // Build all candidate blocks of (blockSize) TEACHING slots from the
                // full-day list, allowing break slots to sit between teaching slots.
                List<List<TimeSlot>> candidates = buildLabCandidates(allDaySlots, blockSize);

                for (List<TimeSlot> candidate : candidates) {
                    // The "teaching" slots are the non-break ones
                    List<TimeSlot> teachingInBlock = candidate.stream()
                            .filter(ts -> !ts.isBreakSlot())
                            .collect(Collectors.toList());

                    // Validate: we need exactly blockSize teaching slots
                    if (teachingInBlock.size() != blockSize)
                        continue;

                    // Saturday afternoon restriction (use last teaching slot)
                    if (isSaturdayAfterLunch(candidate))
                        continue;

                    // Check all teaching slots are free (room, faculty, section)
                    for (Room room : allRooms) {
                        if (!roomTypeMatches(room, "PRACTICAL"))
                            continue;
                        if (!hasCapacity(room, offering.getSection()))
                            continue;

                        if (checkResourcesFreeForBlock(offering, room, teachingInBlock,
                                occupied, facultyCreditHours, blockSize)) {
                            // Book every teaching slot
                            for (TimeSlot slot : teachingInBlock) {
                                created.add(save(offering, room, slot));
                                occupy(offering, room, slot, occupied);
                            }
                            incrementHours(offering.getFaculty(), facultyCreditHours, blockSize);
                            sectionDailyHours.merge(
                                    offering.getSection().getId() + "_" + day, blockSize, Integer::sum);
                            placed = true;
                            break;
                        }
                    }
                    if (placed)
                        break outer;
                }
            }

            if (placed)
                scheduledCount++;
        }

        return scheduledCount == neededBlocks;
    }

    /**
     * Build all possible lab candidate windows from a day's full slot list.
     *
     * A candidate is a contiguous run of slots (including at most ONE break
     * in the middle) that contains exactly {@code teachingSlotsNeeded}
     * non-break slots whose wall-clock times are truly consecutive.
     */
    private List<List<TimeSlot>> buildLabCandidates(List<TimeSlot> daySlots, int teachingSlotsNeeded) {
        List<List<TimeSlot>> result = new ArrayList<>();
        int n = daySlots.size();

        for (int start = 0; start < n; start++) {
            // Don't start a lab on a break slot
            if (daySlots.get(start).isBreakSlot())
                continue;

            List<TimeSlot> window = new ArrayList<>();
            int teachingCount = 0;
            int breakCount = 0;

            for (int end = start; end < n && teachingCount < teachingSlotsNeeded; end++) {
                TimeSlot curr = daySlots.get(end);

                // Check wall-clock contiguity with the previous slot
                if (end > start) {
                    TimeSlot prev = daySlots.get(end - 1);
                    if (!curr.getStartTime().equals(prev.getEndTime()))
                        break; // gap
                }

                if (curr.isBreakSlot()) {
                    breakCount++;
                    if (breakCount > 1)
                        break; // only one break allowed per lab block
                } else {
                    teachingCount++;
                }
                window.add(curr);
            }

            if (teachingCount == teachingSlotsNeeded) {
                result.add(new ArrayList<>(window));
            }
        }
        return result;
    }

    // =========================================================================
    // Theory / Tutorial scheduling (unchanged logic, teaching slots only)
    // =========================================================================

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

        for (int b = 0; b < neededBlocks; b++) {
            boolean placed = false;

            List<String> sortedDays = new ArrayList<>(orderedDays());
            sortedDays.sort(Comparator.comparingInt(
                    d -> sectionDailyHours.getOrDefault(offering.getSection().getId() + "_" + d, 0)));

            for (String day : sortedDays) {
                List<TimeSlot> daySlots = slotsByDay.getOrDefault(day, List.of());

                for (int i = 0; i <= daySlots.size() - blockSize; i++) {
                    List<TimeSlot> block = daySlots.subList(i, i + blockSize);

                    if (!isContiguous(block))
                        continue;
                    if (isBreakForSemester(block, offering.getSection().getSemester()))
                        continue;
                    if (isSaturdayAfterLunch(block))
                        continue;

                    // US-19: no three consecutive theory classes for faculty
                    if (hasConsecutiveFacultyClass(offering.getFaculty(), day, block, occupied, daySlots))
                        continue;

                    for (Room room : allRooms) {
                        if (!roomTypeMatches(room, type))
                            continue;
                        if (!hasCapacity(room, offering.getSection()))
                            continue;

                        if (checkResourcesFreeForBlock(offering, room, block,
                                occupied, facultyCreditHours, blockSize)) {
                            for (TimeSlot slot : block) {
                                created.add(save(offering, room, slot));
                                occupy(offering, room, slot, occupied);
                            }
                            incrementHours(offering.getFaculty(), facultyCreditHours, blockSize);
                            sectionDailyHours.merge(
                                    offering.getSection().getId() + "_" + day, blockSize, Integer::sum);
                            placed = true;
                            break;
                        }
                    }
                    if (placed)
                        break;
                }
                if (placed)
                    break;
            }
            if (placed)
                scheduledCount++;
        }

        return scheduledCount == neededBlocks;
    }

    // =========================================================================
    // Constraint helpers
    // =========================================================================

    private boolean hasConsecutiveFacultyClass(Faculty faculty, String day, List<TimeSlot> block,
            Set<String> occupied, List<TimeSlot> daySlots) {
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
        String rType = room.getType() == null ? "" : room.getType().trim().toUpperCase();
        if ("PRACTICAL".equals(type) || "LAB".equals(type))
            return "LAB".equalsIgnoreCase(rType);
        return "CLASSROOM".equalsIgnoreCase(rType) || "THEORY".equalsIgnoreCase(rType);
    }

    private boolean roomTypeMatches(Room room, Course course) {
        String rtype = room.getType() == null ? "" : room.getType().trim().toUpperCase();
        String ctype = courseType(course);
        if (isLab(ctype))
            return "LAB".equalsIgnoreCase(rtype);
        return "CLASSROOM".equalsIgnoreCase(rtype) || "THEORY".equalsIgnoreCase(rtype);
    }

    private boolean hasCapacity(Room room, Section section) {
        Integer capacity = room.getCapacity();
        Integer studentCount = section.getStudentCount();
        int required = (studentCount == null) ? 30 : studentCount;
        return capacity != null && capacity >= required;
    }

    /**
     * Check that room, faculty, and section are free for every slot in the block.
     */
    private boolean checkResourcesFreeForBlock(
            CourseOffering offering,
            Room room,
            List<TimeSlot> slots,
            Set<String> occupied,
            Map<Long, Integer> facultyHours,
            int hoursToAdd) {

        Faculty faculty = offering.getFaculty();
        Section section = offering.getSection();

        int current = facultyHours.getOrDefault(faculty.getId(), 0);
        int limit = getFacultyMaxHours(faculty);
        if (current + hoursToAdd > limit)
            return false;

        for (TimeSlot slot : slots) {
            String day = normalizeDay(slot.getDayOfWeek());
            String roomKey = key("ROOM", room.getId(), day, slot.getStartTime());
            String facultyKey = key("FACULTY", faculty.getId(), day, slot.getStartTime());
            String sectionKey = key("SECTION", section.getId(), day, slot.getStartTime());
            if (occupied.contains(roomKey) || occupied.contains(facultyKey) || occupied.contains(sectionKey))
                return false;
        }
        return true;
    }

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

    /**
     * Returns true if any slot in the block falls inside a break window
     * for the given semester. Used for THEORY slots only (labs are exempt).
     */
    private boolean isBreakForSemester(List<TimeSlot> block, Integer semester) {
        if (semester == null)
            return false;
        for (TimeSlot slot : block) {
            LocalTime start = slot.getStartTime();
            LocalTime end = slot.getEndTime();

            // Sem 3 & 4: tea 10:50–11:10, lunch 13:00–13:55
            if (semester == 3 || semester == 4) {
                if (isOverlapping(start, end, LocalTime.of(10, 50), LocalTime.of(11, 10)))
                    return true;
                if (isOverlapping(start, end, LocalTime.of(13, 0), LocalTime.of(13, 55)))
                    return true;
            }
            // Sem 5, 6, 7: tea 09:55–10:15, lunch 12:05–13:00
            if (semester >= 5 && semester <= 7) {
                if (isOverlapping(start, end, LocalTime.of(9, 55), LocalTime.of(10, 15)))
                    return true;
                if (isOverlapping(start, end, LocalTime.of(12, 5), LocalTime.of(13, 0)))
                    return true;
            }
            // Generic fallback for other semesters
            if (semester < 3 || semester > 7) {
                if (isOverlapping(start, end, LocalTime.of(13, 0), LocalTime.of(13, 55)))
                    return true;
            }
        }
        return false;
    }

    private boolean isOverlapping(LocalTime s1, LocalTime e1, LocalTime s2, LocalTime e2) {
        return s1.isBefore(e2) && s2.isBefore(e1);
    }

    private boolean isSaturdayAfterLunch(List<TimeSlot> block) {
        for (TimeSlot slot : block) {
            if ("SATURDAY".equalsIgnoreCase(normalizeDay(slot.getDayOfWeek()))) {
                if (!slot.getStartTime().isBefore(LocalTime.of(13, 0)))
                    return true;
            }
        }
        return false;
    }

    // =========================================================================
    // Book-keeping helpers
    // =========================================================================

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

    private int labBlockSize(Course c) {
        String cType = courseType(c);
        // LAB-only, pure lab, or skill-enhancement → 3-hour block
        if ("LAB".equalsIgnoreCase(cType) || "SKILL_ENHANCEMENT".equalsIgnoreCase(cType)
                || "LAB_ONLY".equalsIgnoreCase(cType)) {
            return 3;
        }
        // IC (Integrated Course) and others with practicals → 2-hour block
        return 2;
    }

    private int getFacultyMaxHours(Faculty faculty) {
        DesignationConstraint dc = faculty.getDesignationConstraint();
        if (dc == null)
            return Integer.MAX_VALUE;
        int lec = Optional.ofNullable(dc.getMaxLectureHours()).orElse(0);
        int lab = Optional.ofNullable(dc.getMaxLabHours()).orElse(0);
        return lec + lab;
    }

    // =========================================================================
    // Update scheduled class slot (manual drag-and-drop)
    // =========================================================================

    @Transactional
    public ScheduledClass updateScheduledClassSlot(UpdateSlotRequest req) {
        if (req.getScheduledClassId() == null || req.getNewRoomId() == null
                || req.getNewDayOfWeek() == null || req.getNewStartTime() == null) {
            throw new RuntimeException("Missing required fields in request");
        }

        ScheduledClass sc = scheduledClassRepository.findById(req.getScheduledClassId())
                .orElseThrow(() -> new RuntimeException("Scheduled class not found: " + req.getScheduledClassId()));

        Room room = roomRepository.findById(req.getNewRoomId())
                .orElseThrow(() -> new RuntimeException("Room not found: " + req.getNewRoomId()));

        String day = req.getNewDayOfWeek().trim().toUpperCase();

        TimeSlot newSlot = timeSlotRepository.findAllByOrderByDayOfWeekAscStartTimeAsc().stream()
                .filter(ts -> !ts.isBreakSlot())
                .filter(ts -> day.equals(ts.getDayOfWeek().trim().toUpperCase()))
                .filter(ts -> req.getNewStartTime().equals(ts.getStartTime()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Invalid time slot: " + day + " " + req.getNewStartTime()));

        CourseOffering off = sc.getCourseOffering();
        if (off == null || off.getCourse() == null || off.getSection() == null || off.getFaculty() == null)
            throw new RuntimeException("Scheduled class has invalid offering links");

        if (!roomTypeMatches(room, off.getCourse()))
            throw new RuntimeException("Conflict: Room type incompatible with course type");
        if (!hasCapacity(room, off.getSection()))
            throw new RuntimeException("Conflict: Room capacity insufficient for section");

        boolean roomBusy = scheduledClassRepository
                .findByRoom_IdAndDayOfWeekAndStartTime(room.getId(), day, req.getNewStartTime())
                .stream().anyMatch(other -> !Objects.equals(other.getId(), sc.getId()));
        if (roomBusy)
            throw new RuntimeException("Conflict: Room is already occupied at that time");

        List<ScheduledClass> sameTime = scheduledClassRepository.findByDayOfWeekAndStartTime(day,
                req.getNewStartTime());
        boolean facultyBusy = sameTime.stream()
                .anyMatch(other -> !Objects.equals(other.getId(), sc.getId())
                        && other.getCourseOffering() != null
                        && other.getCourseOffering().getFaculty() != null
                        && Objects.equals(other.getCourseOffering().getFaculty().getId(), off.getFaculty().getId()));
        if (facultyBusy)
            throw new RuntimeException("Conflict: Faculty is already scheduled at that time");

        boolean sectionBusy = sameTime.stream()
                .anyMatch(other -> !Objects.equals(other.getId(), sc.getId())
                        && other.getCourseOffering() != null
                        && other.getCourseOffering().getSection() != null
                        && Objects.equals(other.getCourseOffering().getSection().getId(), off.getSection().getId()));
        if (sectionBusy)
            throw new RuntimeException("Conflict: Section is already scheduled at that time");

        sc.setRoom(room);
        sc.setDayOfWeek(day);
        sc.setStartTime(req.getNewStartTime());
        sc.setEndTime(newSlot.getEndTime());
        return scheduledClassRepository.save(sc);
    }

    // =========================================================================
    // Pure utilities
    // =========================================================================

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
        return day == null ? "" : day.trim().toUpperCase();
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
