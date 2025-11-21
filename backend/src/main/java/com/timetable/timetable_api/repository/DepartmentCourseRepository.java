package com.timetable.timetable_api.repository;

import com.timetable.timetable_api.model.DepartmentCourse;
import com.timetable.timetable_api.model.DepartmentCourseId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentCourseRepository extends JpaRepository<DepartmentCourse, DepartmentCourseId> {
    // Primary Key is the 'DepartmentCourseId' class
    List<DepartmentCourse> findByIdDepartmentId(Integer departmentId);

    // Convenience traversal via relation to fetch by Department.id
    List<DepartmentCourse> findByDepartmentId(Integer departmentId);
}
