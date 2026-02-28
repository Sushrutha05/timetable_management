package com.timetable.timetable_api.repository;

import com.timetable.timetable_api.model.Faculty; // Import your Faculty model
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FacultyRepository extends JpaRepository<Faculty, Long> {
    Faculty findByUserId(Long userId);

    java.util.List<Faculty> findByDepartmentId(Integer departmentId);
}