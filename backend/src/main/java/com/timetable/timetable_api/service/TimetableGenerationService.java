package com.timetable.timetable_api.service;

import com.timetable.timetable_api.model.*;
import com.timetable.timetable_api.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.*;

@Service
public class TimetableGenerationService {

    @Autowired
    private CourseOfferingRepository offeringRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private ScheduledClassRepository scheduledClassRepository;

    // Define a simple list of time slots to try
    private static final List<String> DAYS = List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");
    private static final List<LocalTime> START_TIMES = List.of(
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            LocalTime.of(12, 0),
            LocalTime.of(13, 0), // Lunch break (or 1-hour class)
            LocalTime.of(14, 0),
            LocalTime.of(15, 0),
            LocalTime.of(16, 0)
    );

    @Transactional
    public List<ScheduledClass> generateTimetable() {
        // 1. Clear any existing timetable
        scheduledClassRepository.deleteAll();

        // 2. Get all inputs
        List<CourseOffering> offeringsToSchedule = offeringRepository.findAll();
        List<Room> allRooms = roomRepository.findAll();
        List<ScheduledClass> generatedClasses = new ArrayList<>();

        // This Set will track "booked" slots to prevent clashes.
        // Key format: "TYPE_ID_DAY_TIME"
        // e.g., "FACULTY_1_MONDAY_09:00"
        Set<String> occupiedSlots = new HashSet<>();

        // 3. Loop through each offering and try to schedule it
        for (CourseOffering offering : offeringsToSchedule) {
            boolean scheduled = false;

            // Get the entities we need to check for clashes
            Faculty faculty = offering.getFaculty();
            Section section = offering.getSection();

            for (String day : DAYS) {
                for (LocalTime startTime : START_TIMES) {

                    LocalTime endTime = startTime.plusHours(1); // Assuming 1-hour classes

                    // Check if this faculty, section, AND room are free
                    for (Room room : allRooms) {

                        // --- CHECK CONSTRAINTS ---
                        // 1. Is the ROOM free at this time?
                        String roomSlot = "ROOM_" + room.getId() + "_" + day + "_" + startTime;
                        if (occupiedSlots.contains(roomSlot)) {
                            continue; // This room is booked, try next room
                        }

                        // 2. Is the FACULTY free at this time?
                        String facultySlot = "FACULTY_" + faculty.getId() + "_" + day + "_" + startTime;
                        if (occupiedSlots.contains(facultySlot)) {
                            continue; // Faculty is busy, try next room (in case of a different slot)
                        }

                        // 3. Is the SECTION free at this time?
                        String sectionSlot = "SECTION_" + section.getId() + "_" + day + "_" + startTime;
                        if (occupiedSlots.contains(sectionSlot)) {
                            continue; // Section is busy, try next room
                        }

                        // --- ALL CHECKS PASSED ---
                        // We found a slot! Book it.

                        ScheduledClass newClass = new ScheduledClass();
                        newClass.setCourseOffering(offering);
                        newClass.setRoom(room);
                        newClass.setDayOfWeek(day);
                        newClass.setStartTime(startTime);
                        newClass.setEndTime(endTime);

                        // Save to database
                        scheduledClassRepository.save(newClass);
                        generatedClasses.add(newClass);

                        // Mark slots as "occupied"
                        occupiedSlots.add(roomSlot);
                        occupiedSlots.add(facultySlot);
                        occupiedSlots.add(sectionSlot);

                        scheduled = true;
                        break; // Stop checking rooms
                    }
                    if (scheduled) break; // Stop checking start times
                }
                if (scheduled) break; // Stop checking days
            }

            if (!scheduled) {
                // Could not schedule this offering, throw error or log it
                System.out.println("Warning: Could not schedule offering ID: " + offering.getId());
            }
        }
        return generatedClasses;
    }
}