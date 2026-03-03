package com.timetable.timetable_api.repository;

import com.timetable.timetable_api.model.DesignationConstraint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DesignationConstraintRepository extends JpaRepository<DesignationConstraint, String> {
    // Primary Key 'designation' is a String
    Optional<DesignationConstraint> findByDesignationIgnoreCase(String designation);
}