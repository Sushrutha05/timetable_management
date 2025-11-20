package com.timetable.timetable_api.repository;

import com.timetable.timetable_api.model.DepartmentCourse;
import com.timetable.timetable_api.model.DepartmentCourseId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentCourseRepository extends JpaRepository<DepartmentCourse, DepartmentCourseId> {
    // Primary Key is the 'DepartmentCourseId' class
}