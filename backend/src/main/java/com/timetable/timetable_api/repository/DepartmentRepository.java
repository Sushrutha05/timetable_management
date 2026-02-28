package com.timetable.timetable_api.repository;

import com.timetable.timetable_api.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {
    boolean existsByNameIgnoreCase(String name);

    Optional<Department> findByNameIgnoreCase(String name);
}