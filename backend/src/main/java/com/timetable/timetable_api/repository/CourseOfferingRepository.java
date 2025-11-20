package com.timetable.timetable_api.repository;

import com.timetable.timetable_api.model.CourseOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {
    // Primary Key 'offering_id' is a Long
}