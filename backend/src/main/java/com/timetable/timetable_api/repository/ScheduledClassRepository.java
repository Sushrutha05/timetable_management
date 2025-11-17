package com.timetable.timetable_api.repository;

import com.timetable.timetable_api.model.ScheduledClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // Add this import

@Repository
public interface ScheduledClassRepository extends JpaRepository<ScheduledClass, Long> {
    // Primary Key 'class_id' is a Long

    // --- ADD THIS METHOD ---
    // Spring Data JPA will automatically build a query that joins
    // ScheduledClass -> CourseOffering -> Faculty -> ID
    List<ScheduledClass> findByCourseOffering_Faculty_Id(Long facultyId);
}