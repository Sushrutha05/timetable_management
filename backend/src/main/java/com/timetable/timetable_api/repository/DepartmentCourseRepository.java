package com.timetable.timetable_api.repository;

import com.timetable.timetable_api.model.DepartmentCourse;
import com.timetable.timetable_api.model.DepartmentCourseId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentCourseRepository extends JpaRepository<DepartmentCourse, DepartmentCourseId> {
    List<DepartmentCourse> findByIdDepartmentId(Integer departmentId);

    List<DepartmentCourse> findByDepartmentId(Integer departmentId);

    List<DepartmentCourse> findByDepartmentIdAndSemester(Integer departmentId, Integer semester);

    long countByDepartmentId(Integer departmentId);

    // Used by deleteCourse to remove all department links before deleting the
    // course
    void deleteByCourseId(Long courseId);
}
