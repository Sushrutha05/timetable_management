package com.timetable.timetable_api.service;

import com.timetable.timetable_api.dto.FacultyPreferenceRequest;
import com.timetable.timetable_api.model.Course;
import com.timetable.timetable_api.model.Faculty;
import com.timetable.timetable_api.model.FacultyPreference;
import com.timetable.timetable_api.repository.CourseRepository;
import com.timetable.timetable_api.repository.FacultyPreferenceRepository;
import com.timetable.timetable_api.repository.FacultyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FacultyPreferenceService {

    @Autowired
    private FacultyPreferenceRepository preferenceRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private CourseRepository courseRepository;

    /**
     * Gets all preferences for a specific faculty member.
     */
    public List<FacultyPreference> getPreferencesByFaculty(Long facultyId) {
        // We'll add a custom method to the repository for this
        return preferenceRepository.findByFacultyId(facultyId);
    }

    /**
     * Overwrites all preferences for a faculty member.
     */
    @Transactional
    public List<FacultyPreference> setPreferences(Long facultyId, FacultyPreferenceRequest request) {

        // 1. Fetch the faculty member
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found with ID: " + facultyId));

        // 2. Delete all old preferences for this faculty
        preferenceRepository.deleteByFacultyId(facultyId);

        // 3. Create and save new preferences
        return request.getPreferences().stream().map(item -> {
            // Fetch the course
            Course course = courseRepository.findById(item.getCourseId())
                    .orElseThrow(() -> new RuntimeException("Course not found with ID: " + item.getCourseId()));

            FacultyPreference newPreference = new FacultyPreference();
            newPreference.setFaculty(faculty);
            newPreference.setCourse(course);
            newPreference.setPriority(item.getPriority());

            return preferenceRepository.save(newPreference);
        }).collect(Collectors.toList());
    }
}