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

    // =========================================================================
    // Master timetable generation
    // =========================================================================

    /**
     * Generates a master timetable covering every section in every semester.
     *
     * Strategy — section-first processing:
     * for each semesterGroup (SEM_1_2, SEM_3_4, SEM_5_6_7, …)
     * for each section in that group
     * schedule ALL offerings for that section:
     * 1. Practicals first (highest priority — hardest to fit)
     * 2. Lectures
     * 3. Tutorials
     *
     * Why section-first?
     * When sections were interleaved by semester, CSE1 would grab all available
     * lab slots (same day/room) before CSE2–4 had a chance. Section-first ensures
     * each section secures its lab blocks completely before the next section
     * starts.
     *
     * Rooms and faculty are still tracked globally, so cross-section resource
     * conflicts (same room or same faculty double-booked) are always prevented.
     *
     * Lab blocks may span ONE tea-break slot — a break slot sitting inside a lab
     * period is transparent to students but is NOT saved as a ScheduledClass.
     */
    @Transactional
    public List<ScheduledClass> generateTimetable() {

        // ── Phase A: reset state ───────────────────────────────────────────
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
            throw new RuntimeException(
                    "No rooms configured. Please add rooms before generating a timetable.");

        // ── Phase B: build per-semesterGroup slot maps ─────────────────────
        //
        // Each group (SEM_3_4, SEM_5_6_7, …) has its own slot grid with
        // different break positions. Mixing them causes wall-clock gaps that
        // make every lab block look non-contiguous, so we keep them separate.
        //
        // allByGroup : group → day → ALL slots (incl. breaks) → for lab search
        // teachingByGroup: group → day → non-break slots only → for theory/tutorial
        List<TimeSlot> allSlotsRaw = timeSlotRepository.findAllByOrderByDayOfWeekAscStartTimeAsc()
                .stream()
                .sorted(Comparator
                        .comparing((TimeSlot ts) -> dayOrder(ts.getDayOfWeek()))
                        .thenComparing(TimeSlot::getStartTime))
                .collect(Collectors.toList());

        if (allSlotsRaw.isEmpty())
            throw new RuntimeException(
                    "No time slots configured. Please create time slots before generating.");

        Map<String, Map<String, List<TimeSlot>>> allByGroup = new LinkedHashMap<>();
        Map<String, Map<String, List<TimeSlot>>> teachingByGroup = new LinkedHashMap<>();

        for (TimeSlot ts : allSlotsRaw) {
            String grp = ts.getSemesterGroup() == null
                    ? "UNKNOWN"
                    : ts.getSemesterGroup().trim().toUpperCase();
            String day = normalizeDay(ts.getDayOfWeek());

            allByGroup.computeIfAbsent(grp, k -> new LinkedHashMap<>())
                    .computeIfAbsent(day, k -> new ArrayList<>()).add(ts);
            if (!ts.isBreakSlot())
                teachingByGroup.computeIfAbsent(grp, k -> new LinkedHashMap<>())
                        .computeIfAbsent(day, k -> new ArrayList<>()).add(ts);
        }

        // ── Phase C: global trackers (shared across all sections) ──────────
        Map<Long, Integer> facultyHours = new HashMap<>();
        Map<String, Integer> sectionDailyHours = new HashMap<>();
        Set<String> occupied = new HashSet<>();
        List<ScheduledClass> created = new ArrayList<>();

        // ── Phase D: section-first scheduling ─────────────────────────────
        // Group all offerings by their section
        Map<Long, List<CourseOffering>> bySection = allOfferings.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getSection().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        // Build a stable section processing order:
        // semesterGroup ascending → semester number ascending → section ID ascending
        List<Long> sectionOrder = new ArrayList<>(bySection.keySet());
        sectionOrder.sort(Comparator
                .<Long, String>comparing(sid -> {
                    Section s = bySection.get(sid).get(0).getSection();
                    return semesterGroupFor(s.getSemester());
                })
                .thenComparingInt(sid -> bySection.get(sid).get(0).getSection().getSemester())
                .thenComparingLong(sid -> sid));

        for (Long sectionId : sectionOrder) {
            List<CourseOffering> sectionOfferings = bySection.get(sectionId);
            Section section = sectionOfferings.get(0).getSection();

            String grp = semesterGroupFor(section.getSemester());
            Map<String, List<TimeSlot>> grpAllByDay = allByGroup.getOrDefault(grp, Map.of());
            Map<String, List<TimeSlot>> grpTeachByDay = teachingByGroup.getOrDefault(grp, Map.of());

            if (grpAllByDay.isEmpty())
                throw new RuntimeException(
                        "No time slots found for semesterGroup='" + grp
                                + "' (section=" + section.getName()
                                + ", semester=" + section.getSemester() + "). "
                                + "Please configure time slots for this semester group "
                                + "in Manage Time Slots.");

            // Within each section: practicals first (hardest to place), then by credits
            sectionOfferings.sort((a, b) -> {
                int pA = Optional.ofNullable(a.getCourse().getPracticalHours()).orElse(0);
                int pB = Optional.ofNullable(b.getCourse().getPracticalHours()).orElse(0);
                if (pA != pB)
                    return Integer.compare(pB, pA);
                return Integer.compare(
                        Optional.ofNullable(b.getCourse().getCreditHours()).orElse(0),
                        Optional.ofNullable(a.getCourse().getCreditHours()).orElse(0));
            });

            for (CourseOffering offering : sectionOfferings) {
                Course c = offering.getCourse();
                if (c == null)
                    continue;

                int pHours = Optional.ofNullable(c.getPracticalHours()).orElse(0);
                int lHours = Optional.ofNullable(c.getLectureHours()).orElse(0);
                int tHours = Optional.ofNullable(c.getTutorialHours()).orElse(0);

                // Fallback: treat credit hours as lectures if L/T/P all unset
                if (lHours == 0 && tHours == 0 && pHours == 0)
                    lHours = Optional.ofNullable(c.getCreditHours()).orElse(0);

                // 1. Practicals — full slot list (including breaks) so a
                // tea-break inside the lab block is transparent
                if (pHours > 0) {
                    int blockSize = labBlockSize(c);
                    boolean ok = schedulePractical(
                            offering, pHours, blockSize,
                            allRooms, grpAllByDay,
                            occupied, facultyHours, sectionDailyHours, created);
                    if (!ok)
                        throw new RuntimeException(
                                "Failed to schedule Practical for " + c.getCourseCode()
                                        + " | section=" + section.getName()
                                        + " | semGroup=" + grp
                                        + " | blockSize=" + blockSize
                                        + " | pHours=" + pHours
                                        + " — no free lab room + contiguous slot window found."
                                        + " Check: (a) lab rooms exist and have enough capacity,"
                                        + " (b) time slots for " + grp + " are configured,"
                                        + " (c) faculty max-hours are not already exhausted.");
                }

                // 2. Lectures — teaching slots only
                if (lHours > 0) {
                    boolean ok = scheduleComponent(
                            offering, "LECTURE", lHours, 1,
                            allRooms, grpTeachByDay,
                            occupied, facultyHours, sectionDailyHours, created);
                    if (!ok)
                        throw new RuntimeException(
                                "Failed to schedule Lecture for " + c.getCourseCode()
                                        + " | section=" + section.getName()
                                        + " | semGroup=" + grp);
                }

                // 3. Tutorials — teaching slots only
                if (tHours > 0) {
                    boolean ok = scheduleComponent(
                            offering, "TUTORIAL", tHours, 1,
                            allRooms, grpTeachByDay,
                            occupied, facultyHours, sectionDailyHours, created);
                    if (!ok)
                        throw new RuntimeException(
                                "Failed to schedule Tutorial for " + c.getCourseCode()
                                        + " | section=" + section.getName()
                                        + " | semGroup=" + grp);
                }
            }
        }

        return created;
    }

    // =========================================================================
    // Practical / lab scheduling — break-spanning aware
    // =========================================================================

    /**
     * Schedule all practical blocks for one offering.
     *
     * A lab block consists of {@code blockSize} TEACHING slots. Up to one
     * break slot may sit between teaching slots (the tea-break inside a
     * 3-hour lab session). The break slot is NOT saved as a ScheduledClass.
     */
    private boolean schedulePractical(
            CourseOffering offering,
            int totalPracticalHours,
            int blockSize,
            List<Room> allRooms,
            Map<String, List<TimeSlot>> allByDay,
            Set<String> occupied,
            Map<Long, Integer> facultyHours,
            Map<String, Integer> sectionDailyHours,
            List<ScheduledClass> created) {

        int neededBlocks = (int) Math.ceil((double) totalPracticalHours / blockSize);
        int scheduledCount = 0;

        for (int b = 0; b < neededBlocks; b++) {
            boolean placed = false;

            // Try least-loaded day first for this section
            List<String> sortedDays = new ArrayList<>(orderedDays());
            sortedDays.sort(Comparator.comparingInt(
                    d -> sectionDailyHours.getOrDefault(
                            offering.getSection().getId() + "_" + d, 0)));

            outer: for (String day : sortedDays) {
                List<TimeSlot> dayAll = allByDay.getOrDefault(day, List.of());
                List<List<TimeSlot>> candidates = buildLabCandidates(dayAll, blockSize);

                for (List<TimeSlot> candidate : candidates) {
                    List<TimeSlot> teaching = candidate.stream()
                            .filter(ts -> !ts.isBreakSlot())
                            .collect(Collectors.toList());
                    if (teaching.size() != blockSize)
                        continue;
                    if (isSaturdayAfterLunch(candidate))
                        continue;

                    for (Room room : allRooms) {
                        if (!roomTypeMatches(room, "PRACTICAL"))
                            continue;
                        if (!hasCapacity(room, offering.getSection()))
                            continue;

                        if (resourcesFree(offering, room, teaching, occupied, facultyHours, blockSize)) {
                            for (TimeSlot slot : teaching) {
                                created.add(save(offering, room, slot));
                                occupy(offering, room, slot, occupied);
                            }
                            incrementHours(offering.getFaculty(), facultyHours, blockSize);
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
     * Build all candidate lab windows from {@code daySlots} (which includes
     * break slots) that contain exactly {@code teachingNeeded} non-break slots
     * with wall-clock contiguity and at most one break in the middle.
     */
    private List<List<TimeSlot>> buildLabCandidates(List<TimeSlot> daySlots, int teachingNeeded) {
        List<List<TimeSlot>> result = new ArrayList<>();
        int n = daySlots.size();

        for (int start = 0; start < n; start++) {
            if (daySlots.get(start).isBreakSlot())
                continue; // must start on a teaching slot

            List<TimeSlot> window = new ArrayList<>();
            int teachCount = 0;
            int breakCount = 0;

            for (int end = start; end < n && teachCount < teachingNeeded; end++) {
                TimeSlot curr = daySlots.get(end);

                // Wall-clock must be strictly contiguous
                if (end > start) {
                    TimeSlot prev = daySlots.get(end - 1);
                    if (!curr.getStartTime().equals(prev.getEndTime()))
                        break;
                }

                if (curr.isBreakSlot()) {
                    breakCount++;
                    if (breakCount > 1)
                        break; // only one break allowed per lab block
                } else {
                    teachCount++;
                }
                window.add(curr);
            }

            if (teachCount == teachingNeeded) {
                result.add(new ArrayList<>(window));
            }
        }
        return result;
    }

    // =========================================================================
    // Theory / Tutorial scheduling (teaching slots only)
    // =========================================================================

    private boolean scheduleComponent(
            CourseOffering offering,
            String type,
            int totalHours,
            int blockSize,
            List<Room> allRooms,
            Map<String, List<TimeSlot>> slotsByDay,
            Set<String> occupied,
            Map<Long, Integer> facultyHours,
            Map<String, Integer> sectionDailyHours,
            List<ScheduledClass> created) {

        int neededBlocks = (int) Math.ceil((double) totalHours / blockSize);
        int scheduledCount = 0;

        for (int b = 0; b < neededBlocks; b++) {
            boolean placed = false;

            List<String> sortedDays = new ArrayList<>(orderedDays());
            sortedDays.sort(Comparator.comparingInt(
                    d -> sectionDailyHours.getOrDefault(
                            offering.getSection().getId() + "_" + d, 0)));

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
                    if (hasConsecutiveFacultyClass(offering.getFaculty(), day, block, occupied, daySlots))
                        continue;

                    for (Room room : allRooms) {
                        if (!roomTypeMatches(room, type))
                            continue;
                        if (!hasCapacity(room, offering.getSection()))
                            continue;

                        if (resourcesFree(offering, room, block, occupied, facultyHours, blockSize)) {
                            for (TimeSlot slot : block) {
                                created.add(save(offering, room, slot));
                                occupy(offering, room, slot, occupied);
                            }
                            incrementHours(offering.getFaculty(), facultyHours, blockSize);
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
        int firstIdx = daySlots.indexOf(block.get(0));
        int lastIdx = daySlots.indexOf(block.get(block.size() - 1));

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
        int required = section.getStudentCount() == null ? 30 : section.getStudentCount();
        return room.getCapacity() != null && room.getCapacity() >= required;
    }

    private boolean resourcesFree(
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
            if (occupied.contains(key("ROOM", room.getId(), day, slot.getStartTime())))
                return false;
            if (occupied.contains(key("FACULTY", faculty.getId(), day, slot.getStartTime())))
                return false;
            if (occupied.contains(key("SECTION", section.getId(), day, slot.getStartTime())))
                return false;
        }
        return true;
    }

    private boolean isContiguous(List<TimeSlot> block) {
        if (block.size() < 2)
            return true;
        String day = normalizeDay(block.get(0).getDayOfWeek());
        for (int i = 0; i < block.size() - 1; i++) {
            TimeSlot a = block.get(i), b = block.get(i + 1);
            if (a.isBreakSlot() || b.isBreakSlot())
                return false;
            if (!normalizeDay(a.getDayOfWeek()).equals(day))
                return false;
            if (!a.getEndTime().equals(b.getStartTime()))
                return false;
        }
        return true;
    }

    /**
     * Returns true if any slot in the block falls inside a break window for the
     * given semester.
     * Used for THEORY/TUTORIAL slots only — labs are exempt (they span breaks
     * intentionally).
     */
    private boolean isBreakForSemester(List<TimeSlot> block, Integer semester) {
        if (semester == null)
            return false;
        for (TimeSlot slot : block) {
            LocalTime s = slot.getStartTime(), e = slot.getEndTime();
            if (semester == 3 || semester == 4) {
                if (overlaps(s, e, LocalTime.of(10, 50), LocalTime.of(11, 10)))
                    return true;
                if (overlaps(s, e, LocalTime.of(13, 0), LocalTime.of(13, 55)))
                    return true;
            } else if (semester >= 5 && semester <= 7) {
                if (overlaps(s, e, LocalTime.of(9, 55), LocalTime.of(10, 15)))
                    return true;
                if (overlaps(s, e, LocalTime.of(12, 5), LocalTime.of(13, 0)))
                    return true;
            } else {
                if (overlaps(s, e, LocalTime.of(13, 0), LocalTime.of(13, 55)))
                    return true;
            }
        }
        return false;
    }

    private boolean overlaps(LocalTime s1, LocalTime e1, LocalTime s2, LocalTime e2) {
        return s1.isBefore(e2) && s2.isBefore(e1);
    }

    private boolean isSaturdayAfterLunch(List<TimeSlot> block) {
        for (TimeSlot slot : block) {
            if ("SATURDAY".equalsIgnoreCase(normalizeDay(slot.getDayOfWeek()))
                    && !slot.getStartTime().isBefore(LocalTime.of(13, 0)))
                return true;
        }
        return false;
    }

    // =========================================================================
    // Booking helpers
    // =========================================================================

    private void occupy(CourseOffering offering, Room room, TimeSlot slot, Set<String> occupied) {
        String day = normalizeDay(slot.getDayOfWeek());
        occupied.add(key("ROOM", room.getId(), day, slot.getStartTime()));
        occupied.add(key("FACULTY", offering.getFaculty().getId(), day, slot.getStartTime()));
        occupied.add(key("SECTION", offering.getSection().getId(), day, slot.getStartTime()));
    }

    private void incrementHours(Faculty faculty, Map<Long, Integer> hours, int inc) {
        hours.merge(faculty.getId(), inc, Integer::sum);
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
        return ("LAB".equalsIgnoreCase(cType)
                || "LAB_ONLY".equalsIgnoreCase(cType)
                || "SKILL_ENHANCEMENT".equalsIgnoreCase(cType)) ? 3 : 2;
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
    // Manual slot update (drag-and-drop)
    // =========================================================================

    @Transactional
    public ScheduledClass updateScheduledClassSlot(UpdateSlotRequest req) {
        if (req.getScheduledClassId() == null || req.getNewRoomId() == null
                || req.getNewDayOfWeek() == null || req.getNewStartTime() == null)
            throw new RuntimeException("Missing required fields in request");

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
                .stream().anyMatch(o -> !Objects.equals(o.getId(), sc.getId()));
        if (roomBusy)
            throw new RuntimeException("Conflict: Room is already occupied at that time");

        List<ScheduledClass> same = scheduledClassRepository.findByDayOfWeekAndStartTime(day, req.getNewStartTime());
        boolean fBusy = same.stream().anyMatch(o -> !Objects.equals(o.getId(), sc.getId())
                && o.getCourseOffering() != null && o.getCourseOffering().getFaculty() != null
                && Objects.equals(o.getCourseOffering().getFaculty().getId(), off.getFaculty().getId()));
        if (fBusy)
            throw new RuntimeException("Conflict: Faculty is already scheduled at that time");

        boolean sBusy = same.stream().anyMatch(o -> !Objects.equals(o.getId(), sc.getId())
                && o.getCourseOffering() != null && o.getCourseOffering().getSection() != null
                && Objects.equals(o.getCourseOffering().getSection().getId(), off.getSection().getId()));
        if (sBusy)
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

    /**
     * Maps a semester number to the semesterGroup label stored in
     * time_slots.semester_group.
     * Labels must exactly match what is stored in the database.
     *
     * 1, 2 → SEM_1_2
     * 3, 4 → SEM_3_4
     * 5, 6, 7, 8 → SEM_5_6_7
     */
    private String semesterGroupFor(Integer semester) {
        if (semester == null)
            return "SEM_3_4";
        return switch (semester) {
            case 1, 2 -> "SEM_1_2";
            case 3, 4 -> "SEM_3_4";
            default -> "SEM_5_6_7";
        };
    }
}
