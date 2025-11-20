package com.timetable.timetable_api.repository;

import com.timetable.timetable_api.model.FacultyPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // Import List

@Repository
public interface FacultyPreferenceRepository extends JpaRepository<FacultyPreference, Long> {

    // Spring Data JPA will automatically create this query based on the method name
    List<FacultyPreference> findByFacultyId(Long facultyId);

    // This will automatically create a DELETE query
    void deleteByFacultyId(Long facultyId);
}