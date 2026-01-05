package com.timetable.timetable_api.repository;

import com.timetable.timetable_api.model.ScheduledClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // Add this import

@Repository
public interface ScheduledClassRepository extends JpaRepository<ScheduledClass, Long> {
    // Primary Key 'class_id' is a Long

    // Existing helper queries
    List<ScheduledClass> findByCourseOffering_Faculty_Id(Long facultyId);
    List<ScheduledClass> findByCourseOffering_Section_Id(Long sectionId);

    // Conflict checks for a specific day + start time
    List<ScheduledClass> findByDayOfWeekAndStartTime(String dayOfWeek, java.time.LocalTime startTime);

    // Room-time conflicts on a specific day/time
    List<ScheduledClass> findByRoom_IdAndDayOfWeekAndStartTime(Integer roomId, String dayOfWeek, java.time.LocalTime startTime);
}