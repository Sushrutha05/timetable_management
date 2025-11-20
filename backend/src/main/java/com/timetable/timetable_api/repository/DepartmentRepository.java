package com.timetable.timetable_api.repository;

import com.timetable.timetable_api.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {
    // Primary Key 'department_id' is an Integer
}