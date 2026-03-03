package com.timetable.timetable_api.repository;

import com.timetable.timetable_api.model.FacultyPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // Import List

@Repository
public interface FacultyPreferenceRepository extends JpaRepository<FacultyPreference, Long> {

    List<FacultyPreference> findByFacultyId(Long facultyId);

    void deleteByFacultyId(Long facultyId);

    List<FacultyPreference> findByCourseIdOrderByPriorityAsc(Long courseId);

    void deleteByCourseId(Long courseId);
}