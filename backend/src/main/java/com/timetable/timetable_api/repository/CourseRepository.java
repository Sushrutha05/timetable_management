package com.timetable.timetable_api.repository;

import com.timetable.timetable_api.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    // Primary Key 'course_id' is a Long
    Optional<Course> findByCourseCode(String courseCode);
}