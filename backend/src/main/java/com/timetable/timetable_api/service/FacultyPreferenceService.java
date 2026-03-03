package com.timetable.timetable_api.service;

import com.timetable.timetable_api.dto.FacultyPreferenceRequest;
import com.timetable.timetable_api.model.Course;
import com.timetable.timetable_api.model.DepartmentCourse;
import com.timetable.timetable_api.model.Faculty;
import com.timetable.timetable_api.model.FacultyPreference;
import com.timetable.timetable_api.repository.CourseRepository;
import com.timetable.timetable_api.repository.DepartmentCourseRepository;
import com.timetable.timetable_api.repository.FacultyPreferenceRepository;
import com.timetable.timetable_api.repository.FacultyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
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

    @Autowired
    private DepartmentCourseRepository departmentCourseRepository;

    /**
     * Gets all preferences for a specific faculty member.
     */
    public List<FacultyPreference> getPreferencesByFaculty(Long facultyId) {
        return preferenceRepository.findByFacultyId(facultyId);
    }

    /**
     * Overwrites all preferences for a faculty member.
     */
    @Transactional
    public List<FacultyPreference> setPreferences(Long facultyId, FacultyPreferenceRequest request) {
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found with ID: " + facultyId));

        preferenceRepository.deleteByFacultyId(facultyId);

        return request.getPreferences().stream().map(item -> {
            Course course = courseRepository.findById(item.getCourseId())
                    .orElseThrow(() -> new RuntimeException("Course not found with ID: " + item.getCourseId()));

            FacultyPreference newPreference = new FacultyPreference();
            newPreference.setFaculty(faculty);
            newPreference.setCourse(course);
            newPreference.setPriority(item.getPriority());

            return preferenceRepository.save(newPreference);
        }).collect(Collectors.toList());
    }

    /**
     * One-click random preference assignment for a whole department.
     * For every faculty member in [deptId]:
     * 1. Clears their existing preferences.
     * 2. Randomly shuffles all courses taught by that department.
     * 3. Assigns the first MAX_PREFS courses with priorities 1…N.
     *
     * @return total number of preference rows created
     */
    @Transactional
    public int randomizePreferencesForDepartment(Integer deptId) {
        final int MAX_PREFS = 5;

        // All unique courses in this department (across all semesters)
        List<Course> allCourses = departmentCourseRepository.findByDepartmentId(deptId)
                .stream()
                .map(DepartmentCourse::getCourse)
                .distinct()
                .collect(Collectors.toList());

        if (allCourses.isEmpty()) {
            return 0;
        }

        List<Faculty> facultyList = facultyRepository.findByDepartmentId(deptId);
        int totalAssigned = 0;

        for (Faculty faculty : facultyList) {
            preferenceRepository.deleteByFacultyId(faculty.getId());

            List<Course> shuffled = new ArrayList<>(allCourses);
            Collections.shuffle(shuffled);
            List<Course> chosen = shuffled.subList(0, Math.min(MAX_PREFS, shuffled.size()));

            for (int i = 0; i < chosen.size(); i++) {
                FacultyPreference pref = new FacultyPreference();
                pref.setFaculty(faculty);
                pref.setCourse(chosen.get(i));
                pref.setPriority(i + 1); // 1 = highest priority
                preferenceRepository.save(pref);
                totalAssigned++;
            }
        }
        return totalAssigned;
    }
}