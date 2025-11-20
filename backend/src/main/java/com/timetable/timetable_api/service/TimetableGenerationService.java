package com.timetable.timetable_api.service;

import com.timetable.timetable_api.model.*;
import com.timetable.timetable_api.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.*;

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

    @Transactional
    public List<ScheduledClass> generateTimetable() {
        scheduledClassRepository.deleteAll();

        List<CourseOffering> offeringsToSchedule = offeringRepository.findAll();
        List<Room> allRooms = roomRepository.findAll();
        List<TimeSlot> timeSlots = timeSlotRepository.findAll();

        if (allRooms.isEmpty()) {
            throw new RuntimeException("No rooms configured. Please add rooms before generating a timetable.");
        }
        if (timeSlots.isEmpty()) {
            throw new RuntimeException("No time slots configured. Please create time slots before generating a timetable.");
        }

        timeSlots.sort(Comparator
                .comparing((TimeSlot slot) -> dayOrder(slot.getDayOfWeek()))
                .thenComparing(TimeSlot::getStartTime));

        Map<Faculty, Integer> facultyHours = new HashMap<>();
        Set<String> occupiedSlots = new HashSet<>();
        List<ScheduledClass> generatedClasses = new ArrayList<>();

        for (CourseOffering offering : offeringsToSchedule) {
            Course course = offering.getCourse();
            String courseType = course.getCourseType() == null ? "THEORY" : course.getCourseType();
            boolean isLab = "LAB".equalsIgnoreCase(courseType);

            boolean scheduled = isLab
                    ? scheduleLabOffering(offering, allRooms, timeSlots, facultyHours, occupiedSlots, generatedClasses)
                    : scheduleTheoryOffering(offering, allRooms, timeSlots, facultyHours, occupiedSlots, generatedClasses);

            if (!scheduled) {
                throw new RuntimeException("Could not schedule " + course.getCourseName() +
                        " for " + offering.getSection().getName() + ". No available slots.");
            }
        }

        return generatedClasses;
    }

    private boolean scheduleTheoryOffering(
            CourseOffering offering,
            List<Room> rooms,
            List<TimeSlot> timeSlots,
            Map<Faculty, Integer> facultyHours,
            Set<String> occupiedSlots,
            List<ScheduledClass> generatedClasses
    ) {
        Course course = offering.getCourse();
        int slotsNeeded = Math.max(1, course.getCreditHours() == null ? 1 : course.getCreditHours());
        int scheduled = 0;

        for (TimeSlot slot : timeSlots) {
            if (slot.isBreakSlot()) {
                continue;
            }

            for (Room room : rooms) {
                if (!isRoomCompatible(room, course)) {
                    continue;
                }
                if (!isSlotAvailable(offering, room, slot, occupiedSlots, facultyHours, 1)) {
                    continue;
                }

                ScheduledClass scheduledClass = createScheduledClass(offering, room, slot);
                generatedClasses.add(scheduledClass);
                markSlot(offering, room, slot, occupiedSlots);
                incrementFacultyHours(offering.getFaculty(), facultyHours, 1);

                scheduled++;
                if (scheduled >= slotsNeeded) {
                    return true;
                }
                break; // Move to the next time slot after booking this one
            }
        }

        return false;
    }

    private boolean scheduleLabOffering(
            CourseOffering offering,
            List<Room> rooms,
            List<TimeSlot> timeSlots,
            Map<Faculty, Integer> facultyHours,
            Set<String> occupiedSlots,
            List<ScheduledClass> generatedClasses
    ) {
        for (int i = 0; i <= timeSlots.size() - 3; i++) {
            List<TimeSlot> block = timeSlots.subList(i, i + 3);
            if (!isValidLabBlock(block)) {
                continue;
            }

            for (Room room : rooms) {
                if (!isRoomCompatible(room, offering.getCourse())) {
                    continue;
                }
                if (!areSlotsAvailableForBlock(offering, room, block, occupiedSlots, facultyHours)) {
                    continue;
                }

                for (TimeSlot slot : block) {
                    ScheduledClass scheduledClass = createScheduledClass(offering, room, slot);
                    generatedClasses.add(scheduledClass);
                    markSlot(offering, room, slot, occupiedSlots);
                    incrementFacultyHours(offering.getFaculty(), facultyHours, 1);
                }
                return true;
            }
        }
        return false;
    }

    private boolean isValidLabBlock(List<TimeSlot> block) {
        if (block.size() != 3) {
            return false;
        }

        String day = block.get(0).getDayOfWeek();
        for (int i = 0; i < block.size() - 1; i++) {
            TimeSlot current = block.get(i);
            TimeSlot next = block.get(i + 1);

            if (current.isBreakSlot() || next.isBreakSlot()) {
                return false;
            }
            if (!day.equalsIgnoreCase(next.getDayOfWeek())) {
                return false;
            }
            if (!current.getEndTime().equals(next.getStartTime())) {
                return false;
            }
        }
        return !block.get(2).isBreakSlot();
    }

    private boolean isRoomCompatible(Room room, Course course) {
        if (room.getType() == null) {
            return false;
        }
        String courseType = course.getCourseType() == null ? "THEORY" : course.getCourseType();
        return room.getType().equalsIgnoreCase(courseType);
    }

    private boolean hasCapacity(Room room, Section section) {
        Integer capacity = room.getCapacity();
        Integer studentCount = section.getStudentCount();
        int required = studentCount == null ? 45 : studentCount;
        return capacity != null && capacity >= required;
    }

    private boolean isSlotAvailable(
            CourseOffering offering,
            Room room,
            TimeSlot slot,
            Set<String> occupiedSlots,
            Map<Faculty, Integer> facultyHours,
            int durationHours
    ) {
        if (slot.isBreakSlot()) {
            return false;
        }
        if (!hasCapacity(room, offering.getSection())) {
            return false;
        }

        String roomKey = composeKey("ROOM", room.getId(), slot);
        String facultyKey = composeKey("FACULTY", offering.getFaculty().getId(), slot);
        String sectionKey = composeKey("SECTION", offering.getSection().getId(), slot);

        if (occupiedSlots.contains(roomKey)
                || occupiedSlots.contains(facultyKey)
                || occupiedSlots.contains(sectionKey)) {
            return false;
        }

        return hasFacultyHoursAvailable(offering.getFaculty(), facultyHours, durationHours);
    }

    private boolean areSlotsAvailableForBlock(
            CourseOffering offering,
            Room room,
            List<TimeSlot> block,
            Set<String> occupiedSlots,
            Map<Faculty, Integer> facultyHours
    ) {
        if (!hasCapacity(room, offering.getSection())) {
            return false;
        }
        if (!hasFacultyHoursAvailable(offering.getFaculty(), facultyHours, block.size())) {
            return false;
        }

        for (TimeSlot slot : block) {
            if (!isSlotAvailable(offering, room, slot, occupiedSlots, facultyHours, block.size())) {
                return false;
            }
        }
        return true;
    }

    private boolean hasFacultyHoursAvailable(Faculty faculty, Map<Faculty, Integer> facultyHours, int hoursToAdd) {
        int current = facultyHours.getOrDefault(faculty, 0);
        int limit = getFacultyLimit(faculty);
        return current + hoursToAdd <= limit;
    }

    private int getFacultyLimit(Faculty faculty) {
        DesignationConstraint constraint = faculty.getDesignationConstraint();
        if (constraint == null || constraint.getMaxLectureHours() == null) {
            return Integer.MAX_VALUE;
        }
        return constraint.getMaxLectureHours();
    }

    private ScheduledClass createScheduledClass(CourseOffering offering, Room room, TimeSlot slot) {
        ScheduledClass scheduledClass = new ScheduledClass();
        scheduledClass.setCourseOffering(offering);
        scheduledClass.setRoom(room);
        scheduledClass.setDayOfWeek(slot.getDayOfWeek());
        scheduledClass.setStartTime(slot.getStartTime());
        scheduledClass.setEndTime(slot.getEndTime());
        return scheduledClassRepository.save(scheduledClass);
    }

    private void markSlot(CourseOffering offering, Room room, TimeSlot slot, Set<String> occupiedSlots) {
        occupiedSlots.add(composeKey("ROOM", room.getId(), slot));
        occupiedSlots.add(composeKey("FACULTY", offering.getFaculty().getId(), slot));
        occupiedSlots.add(composeKey("SECTION", offering.getSection().getId(), slot));
    }

    private void incrementFacultyHours(Faculty faculty, Map<Faculty, Integer> facultyHours, int increment) {
        facultyHours.put(faculty, facultyHours.getOrDefault(faculty, 0) + increment);
    }

    private String composeKey(String type, Number id, TimeSlot slot) {
        return type + "_" + id + "_" + slot.getDayOfWeek() + "_" + slot.getStartTime();
    }

    private int dayOrder(String day) {
        if (day == null) {
            return 8;
        }
        try {
            return DayOfWeek.valueOf(day.toUpperCase()).getValue();
        } catch (IllegalArgumentException ex) {
            return 8;
        }
    }
}