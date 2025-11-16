package com.timetable.timetable_api.repository;

import com.timetable.timetable_api.model.ScheduledClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduledClassRepository extends JpaRepository<ScheduledClass, Long> {
    // Primary Key 'class_id' is a Long
}