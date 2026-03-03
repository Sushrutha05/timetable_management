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
    // Public API
    // =========================================================================

    @Transactional
    public List<ScheduledClass> generateTimetable() {
        return generateTimetable(null);
    }

    /**
     * Generate timetable for EVEN (2,4,6,8) or ODD (1,3,5,7) semesters.
     * Passing null generates all semesters.
     *
     * Only the selected parity's classes are deleted; the other half is preserved.
     * Generation is SECTION-FIRST so each section fully secures its lab slots
     * before the next section starts (preventing partial starvation).
     */
    @Transactional
    public List<ScheduledClass> generateTimetable(String parity) {

        Set<Integer> targetSems = semestersForParity(parity);

        // Delete only this parity's scheduled classes
        if (targetSems == null) {
            scheduledClassRepository.deleteAll();
        } else {
            List<ScheduledClass> toDelete = scheduledClassRepository.findAll().stream()
                    .filter(sc -> sc.getCourseOffering() != null
                            && sc.getCourseOffering().getSection() != null
                            && targetSems.contains(sc.getCourseOffering().getSection().getSemester()))
                    .collect(Collectors.toList());
            scheduledClassRepository.deleteAll(toDelete);
        }

        TimetableMetadata status = metadataRepository.findByKey("STATUS")
                .orElse(new TimetableMetadata("STATUS", "DRAFT"));
        status.setValue("DRAFT");
        metadataRepository.save(status);

        List<CourseOffering> allOfferings = offeringRepository.findAll().stream()
                .filter(o -> o.getSection() != null
                        && (targetSems == null || targetSems.contains(o.getSection().getSemester())))
                .collect(Collectors.toList());

        if (allOfferings.isEmpty())
            return List.of();

        List<Room> allRooms = roomRepository.findAll();
        if (allRooms.isEmpty())
            throw new RuntimeException(
                    "No rooms configured. Please add rooms before generating a timetable.");

        // Build per-semesterGroup slot maps (with breaks for labs, without for theory)
        List<TimeSlot> allSlotsRaw = timeSlotRepository.findAllByOrderByDayOfWeekAscStartTimeAsc()
                .stream()
                .sorted(Comparator
                        .comparing((TimeSlot ts) -> dayOrder(ts.getDayOfWeek()))
                        .thenComparing(TimeSlot::getStartTime))
                .collect(Collectors.toList());

        if (allSlotsRaw.isEmpty())
            throw new RuntimeException("No time slots configured.");

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

        Map<Long, Integer> facultyHours = new HashMap<>();
        Map<String, Integer> sectionDailyHours = new HashMap<>();
        Set<String> occupied = new HashSet<>();
        List<ScheduledClass> created = new ArrayList<>();

        // Group offerings by section and define processing order
        Map<Long, List<CourseOffering>> bySection = allOfferings.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getSection().getId(), LinkedHashMap::new, Collectors.toList()));

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
            Map<String, List<TimeSlot>> grpAll = allByGroup.getOrDefault(grp, Map.of());
            Map<String, List<TimeSlot>> grpTeach = teachingByGroup.getOrDefault(grp, Map.of());

            if (grpAll.isEmpty())
                throw new RuntimeException(
                        "No time slots found for semesterGroup='" + grp
                                + "' (section=" + section.getName()
                                + ", semester=" + section.getSemester() + "). "
                                + "Please configure time slots in Manage Time Slots.");

            // Labs first, then by credits descending
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
                if (lHours == 0 && tHours == 0 && pHours == 0)
                    lHours = Optional.ofNullable(c.getCreditHours()).orElse(0);

                if (pHours > 0) {
                    int blockSize = labBlockSize(c);
                    boolean ok = schedulePractical(offering, pHours, blockSize,
                            allRooms, grpAll, occupied, facultyHours, sectionDailyHours, created);
                    if (!ok) {
                        String diagnosis = diagnosePracticalFailure(
                                offering, blockSize, allRooms, grpAll, facultyHours);
                        throw new RuntimeException(
                                "Cannot schedule Practical for " + c.getCourseCode()
                                        + " | section=" + section.getName()
                                        + " | semGroup=" + grp + "\n→ " + diagnosis);
                    }
                }

                if (lHours > 0) {
                    boolean ok = scheduleComponent(offering, "LECTURE", lHours, 1,
                            allRooms, grpTeach, occupied, facultyHours, sectionDailyHours, created);
                    if (!ok)
                        throw new RuntimeException(
                                "Cannot schedule Lecture for " + c.getCourseCode()
                                        + " | section=" + section.getName() + " | semGroup=" + grp);
                }

                if (tHours > 0) {
                    boolean ok = scheduleComponent(offering, "TUTORIAL", tHours, 1,
                            allRooms, grpTeach, occupied, facultyHours, sectionDailyHours, created);
                    if (!ok)
                        throw new RuntimeException(
                                "Cannot schedule Tutorial for " + c.getCourseCode()
                                        + " | section=" + section.getName() + " | semGroup=" + grp);
                }
            }
        }

        return created;
    }

    // =========================================================================
    // Lab / Practical scheduling — break-spanning aware
    // =========================================================================

    private boolean schedulePractical(
            CourseOffering offering, int totalHours, int blockSize,
            List<Room> allRooms, Map<String, List<TimeSlot>> allByDay,
            Set<String> occupied, Map<Long, Integer> facultyHours,
            Map<String, Integer> sectionDailyHours, List<ScheduledClass> created) {

        int neededBlocks = (int) Math.ceil((double) totalHours / blockSize);
        int scheduledCount = 0;

        for (int b = 0; b < neededBlocks; b++) {
            boolean placed = false;

            List<String> sortedDays = new ArrayList<>(orderedDays());
            sortedDays.sort(Comparator.comparingInt(d -> sectionDailyHours.getOrDefault(
                    offering.getSection().getId() + "_" + d, 0)));

            outer: for (String day : sortedDays) {
                List<TimeSlot> dayAll = allByDay.getOrDefault(day, List.of());
                for (List<TimeSlot> candidate : buildLabCandidates(dayAll, blockSize)) {
                    List<TimeSlot> teaching = candidate.stream()
                            .filter(ts -> !ts.isBreakSlot()).collect(Collectors.toList());
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
     * Runs after schedulePractical fails and returns a specific actionable error
     * message.
     * Checks each blocking condition in priority order.
     */
    private String diagnosePracticalFailure(
            CourseOffering offering, int blockSize,
            List<Room> allRooms, Map<String, List<TimeSlot>> allByDay,
            Map<Long, Integer> facultyHours) {

        Section section = offering.getSection();
        Faculty faculty = offering.getFaculty();

        // (a) Are there any LAB rooms at all?
        List<Room> labRooms = allRooms.stream()
                .filter(r -> roomTypeMatches(r, "PRACTICAL")).collect(Collectors.toList());
        if (labRooms.isEmpty())
            return "NO LAB ROOMS: There are no rooms with Type=LAB configured. "
                    + "Go to Manage Rooms → add at least one room with Type=LAB.";

        // (b) Do lab rooms have enough capacity for this section?
        int required = section.getStudentCount() == null ? 30 : section.getStudentCount();
        int maxCap = labRooms.stream()
                .mapToInt(r -> r.getCapacity() == null ? 0 : r.getCapacity()).max().orElse(0);
        if (maxCap < required)
            return "LAB CAPACITY TOO LOW: Section '" + section.getName() + "' needs capacity "
                    + required + " but the largest lab room only has capacity " + maxCap + ". "
                    + "Go to Manage Rooms and increase a lab room's capacity, "
                    + "or lower the section's student count in Manage Sections.";

        // (c) Is the faculty max-hours limit zero?
        int limit = getFacultyMaxHours(faculty);
        if (limit <= 0)
            return "FACULTY MAX-HOURS = 0: Faculty '"
                    + faculty.getFirstName() + " " + faculty.getLastName()
                    + "' (designation="
                    + (faculty.getDesignationConstraint() != null ? faculty.getDesignationConstraint().getDesignation()
                            : "null")
                    + ") has "
                    + "MaxLectureHours=0 AND MaxLabHours=0. "
                    + "Go to Manage Designations → set positive values for '"
                    + (faculty.getDesignationConstraint() != null ? faculty.getDesignationConstraint().getDesignation()
                            : "null")
                    + "'.";

        // (d) Is the faculty limit smaller than one lab block?
        if (limit < blockSize)
            return "FACULTY MAX-HOURS TOO LOW: Faculty '"
                    + faculty.getFirstName() + " " + faculty.getLastName()
                    + "' max total hours=" + limit
                    + " is less than the lab block size=" + blockSize + ". "
                    + "Increase MaxLabHours for designation '"
                    + (faculty.getDesignationConstraint() != null ? faculty.getDesignationConstraint().getDesignation()
                            : "null")
                    + "' in Manage Designations.";

        // (e) Are there any contiguous lab windows in this group at all?
        long totalWindows = allByDay.values().stream()
                .mapToLong(slots -> buildLabCandidates(slots, blockSize).size()).sum();
        if (totalWindows == 0)
            return "NO CONTIGUOUS TIME SLOTS: Cannot find " + blockSize
                    + " back-to-back non-break periods in the semester group's time slots. "
                    + "In Manage Time Slots make sure that each day has slots where "
                    + "endTime of slot N equals startTime of slot N+1 (no gaps).";

        // (f) Windows exist but everything is blocked by prior assignments
        int usedHours = facultyHours.getOrDefault(faculty.getId(), 0);
        return "ALL " + totalWindows + " SLOT WINDOWS BLOCKED: "
                + "Either every lab room is already occupied at all " + blockSize
                + "-slot windows, or the faculty '"
                + faculty.getFirstName() + " " + faculty.getLastName()
                + "' has used " + usedHours + "/" + limit + " hours "
                + "leaving no room for another " + blockSize + "-hour block. "
                + "Try: (1) add more lab rooms, (2) add more time slots, "
                + "(3) increase faculty MaxLabHours in Manage Designations.";
    }

    /**
     * Build all valid lab candidate windows from {@code daySlots}.
     * A window may contain at most ONE break slot between teaching slots.
     * Wall-clock contiguity is required (endTime[i] == startTime[i+1]).
     */
    private List<List<TimeSlot>> buildLabCandidates(List<TimeSlot> daySlots, int teachingNeeded) {
        List<List<TimeSlot>> result = new ArrayList<>();
        int n = daySlots.size();
        for (int start = 0; start < n; start++) {
            if (daySlots.get(start).isBreakSlot())
                continue;
            List<TimeSlot> window = new ArrayList<>();
            int teachCount = 0, breakCount = 0;
            for (int end = start; end < n && teachCount < teachingNeeded; end++) {
                TimeSlot curr = daySlots.get(end);
                if (end > start && !curr.getStartTime().equals(daySlots.get(end - 1).getEndTime()))
                    break;
                if (curr.isBreakSlot()) {
                    if (++breakCount > 1)
                        break;
                } else {
                    teachCount++;
                }
                window.add(curr);
            }
            if (teachCount == teachingNeeded)
                result.add(new ArrayList<>(window));
        }
        return result;
    }

    // =========================================================================
    // Theory / Tutorial scheduling (teaching slots only)
    // =========================================================================

    private boolean scheduleComponent(
            CourseOffering offering, String type, int totalHours, int blockSize,
            List<Room> allRooms, Map<String, List<TimeSlot>> slotsByDay,
            Set<String> occupied, Map<Long, Integer> facultyHours,
            Map<String, Integer> sectionDailyHours, List<ScheduledClass> created) {

        int neededBlocks = (int) Math.ceil((double) totalHours / blockSize);
        int scheduledCount = 0;

        for (int b = 0; b < neededBlocks; b++) {
            boolean placed = false;

            List<String> sortedDays = new ArrayList<>(orderedDays());
            sortedDays.sort(Comparator.comparingInt(d -> sectionDailyHours.getOrDefault(
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
                    if (hasConsecutiveFacultyClass(
                            offering.getFaculty(), day, block, occupied, daySlots))
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

    private boolean hasConsecutiveFacultyClass(Faculty faculty, String day,
            List<TimeSlot> block, Set<String> occupied, List<TimeSlot> daySlots) {
        long fid = faculty.getId();
        int fi = daySlots.indexOf(block.get(0));
        int li = daySlots.indexOf(block.get(block.size() - 1));
        if (fi > 0 && occupied.contains(
                key("FACULTY", fid, day, daySlots.get(fi - 1).getStartTime())))
            return true;
        if (li >= 0 && li < daySlots.size() - 1 && occupied.contains(
                key("FACULTY", fid, day, daySlots.get(li + 1).getStartTime())))
            return true;
        return false;
    }

    private boolean roomTypeMatches(Room room, String type) {
        String rt = room.getType() == null ? "" : room.getType().trim().toUpperCase();
        if ("PRACTICAL".equals(type) || "LAB".equals(type))
            return "LAB".equalsIgnoreCase(rt);
        return "CLASSROOM".equalsIgnoreCase(rt) || "THEORY".equalsIgnoreCase(rt);
    }

    private boolean roomTypeMatches(Room room, Course course) {
        String rt = room.getType() == null ? "" : room.getType().trim().toUpperCase();
        if (isLab(courseType(course)))
            return "LAB".equalsIgnoreCase(rt);
        return "CLASSROOM".equalsIgnoreCase(rt) || "THEORY".equalsIgnoreCase(rt);
    }

    private boolean hasCapacity(Room room, Section section) {
        int required = section.getStudentCount() == null ? 30 : section.getStudentCount();
        return room.getCapacity() != null && room.getCapacity() >= required;
    }

    private boolean resourcesFree(CourseOffering offering, Room room, List<TimeSlot> slots,
            Set<String> occupied, Map<Long, Integer> facultyHours, int hoursToAdd) {
        Faculty faculty = offering.getFaculty();
        Section section = offering.getSection();
        int current = facultyHours.getOrDefault(faculty.getId(), 0);
        if (current + hoursToAdd > getFacultyMaxHours(faculty))
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
        for (TimeSlot slot : block)
            if ("SATURDAY".equalsIgnoreCase(normalizeDay(slot.getDayOfWeek()))
                    && !slot.getStartTime().isBefore(LocalTime.of(13, 0)))
                return true;
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
        // If both are 0, treat as unlimited (unconfigured) so we don't block everything
        return (lec == 0 && lab == 0) ? Integer.MAX_VALUE : lec + lab;
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

    private Set<Integer> semestersForParity(String parity) {
        if (parity == null || parity.isBlank())
            return null;
        return switch (parity.trim().toUpperCase()) {
            case "EVEN" -> new HashSet<>(Set.of(2, 4, 6, 8));
            case "ODD" -> new HashSet<>(Set.of(1, 3, 5, 7));
            default -> null;
        };
    }

    private String semesterGroupFor(Integer semester) {
        if (semester == null)
            return "SEM_3_4";
        return switch (semester) {
            case 1, 2 -> "SEM_1_2";
            case 3, 4 -> "SEM_3_4";
            default -> "SEM_5_6_7";
        };
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
