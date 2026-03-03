package com.timetable.timetable_api.service;

import com.timetable.timetable_api.dto.CourseOfferingRequest;
import com.timetable.timetable_api.model.Course;
import com.timetable.timetable_api.model.CourseOffering;
import com.timetable.timetable_api.model.DepartmentCourse;
import com.timetable.timetable_api.model.DesignationConstraint;
import com.timetable.timetable_api.model.Faculty;
import com.timetable.timetable_api.model.FacultyPreference;
import com.timetable.timetable_api.model.Section;
import com.timetable.timetable_api.repository.CourseOfferingRepository;
import com.timetable.timetable_api.repository.CourseRepository;
import com.timetable.timetable_api.repository.DepartmentCourseRepository;
import com.timetable.timetable_api.repository.FacultyPreferenceRepository;
import com.timetable.timetable_api.repository.FacultyRepository;
import com.timetable.timetable_api.repository.SectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseOfferingService {

    @Autowired
    private CourseOfferingRepository offeringRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private FacultyRepository facultyRepository;
    @Autowired
    private SectionRepository sectionRepository;
    @Autowired
    private DepartmentCourseRepository departmentCourseRepository;
    @Autowired
    private FacultyPreferenceRepository preferenceRepository;

    /**
     * Creates a new CourseOffering (Admin's official assignment).
     */
    @Transactional
    public CourseOffering createOffering(CourseOfferingRequest request) {
        // 1. Validate and fetch all entities
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found with ID: " + request.getCourseId()));

        Faculty faculty = facultyRepository.findById(request.getFacultyId())
                .orElseThrow(() -> new RuntimeException("Faculty not found with ID: " + request.getFacultyId()));

        Section section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new RuntimeException("Section not found with ID: " + request.getSectionId()));

        // 2. Create the new offering
        CourseOffering newOffering = new CourseOffering();
        newOffering.setCourse(course);
        newOffering.setFaculty(faculty);
        newOffering.setSection(section);

        // You could add validation here, e.g., check if section already has this course
        // Or check if faculty is over-assigned (based on designation constraints)

        return offeringRepository.save(newOffering);
    }

    /**
     * Gets a list of all current offerings.
     */
    public List<CourseOffering> getAllOfferings() {
        return offeringRepository.findAll();
    }

    /**
     * Deletes an offering.
     */
    public void deleteOffering(Long offeringId) {
        if (!offeringRepository.existsById(offeringId)) {
            throw new RuntimeException("Offering not found with ID: " + offeringId);
        }
        offeringRepository.deleteById(offeringId);
    }

    /**
     * One-click auto-generation of CourseOfferings for a department.
     *
     * Algorithm:
     * For each Section in the dept:
     * For each Course mapped to that section's semester:
     * - Skip if offering already exists for (course, section)
     * - Pick best faculty:
     * 1st: faculty with a preference entry for this course (lowest priority #)
     * 2nd: any dept faculty whose total assigned hours < designation
     * max_total_hours
     * 3rd: random dept faculty (fallback)
     * - Create the CourseOffering
     *
     * @return Map with "created" and "skipped" counts
     */
    @Transactional
    public Map<String, Integer> autoGenerateOfferings(Integer deptId) {
        List<Section> sections = sectionRepository.findByDepartmentId(deptId);
        List<Faculty> allFaculty = facultyRepository.findByDepartmentId(deptId);

        if (sections.isEmpty() || allFaculty.isEmpty()) {
            return Map.of("created", 0, "skipped", 0,
                    "reason", 0); // reason placeholder for missing data
        }

        // Build preference index: courseId -> sorted list of (faculty, priority)
        // Fetch all preferences for all faculty in this dept in one go
        Map<Long, List<FacultyPreference>> preferencesByCourse = new HashMap<>();
        for (Faculty f : allFaculty) {
            for (FacultyPreference pref : preferenceRepository.findByFacultyId(f.getId())) {
                preferencesByCourse
                        .computeIfAbsent(pref.getCourse().getId(), k -> new java.util.ArrayList<>())
                        .add(pref);
            }
        }
        // Sort each course's preference list by priority ascending (1 = most preferred)
        preferencesByCourse.values()
                .forEach(list -> list.sort(Comparator.comparingInt(FacultyPreference::getPriority)));

        int created = 0;
        int skipped = 0;

        for (Section section : sections) {
            // Get courses for this section's semester
            List<Course> semesterCourses = departmentCourseRepository
                    .findByDepartmentIdAndSemester(deptId, section.getSemester())
                    .stream()
                    .map(DepartmentCourse::getCourse)
                    .collect(Collectors.toList());

            for (Course course : semesterCourses) {
                // Skip if offering already exists for this (course, section) pair
                if (!offeringRepository.findBySectionIdAndCourseId(section.getId(), course.getId()).isEmpty()) {
                    skipped++;
                    continue;
                }

                Faculty chosen = pickFaculty(course, section, allFaculty, preferencesByCourse);
                if (chosen == null) {
                    skipped++;
                    continue;
                }

                CourseOffering offering = new CourseOffering();
                offering.setCourse(course);
                offering.setSection(section);
                offering.setFaculty(chosen);
                offeringRepository.save(offering);
                created++;
            }
        }

        return Map.of("created", created, "skipped", skipped);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private Faculty pickFaculty(Course course, Section section,
            List<Faculty> allFaculty,
            Map<Long, List<FacultyPreference>> preferencesByCourse) {

        // 1. Try preference-ordered faculty first
        List<FacultyPreference> prefs = preferencesByCourse.getOrDefault(course.getId(), List.of());
        for (FacultyPreference pref : prefs) {
            Faculty candidate = pref.getFaculty();
            if (isWithinHourLimit(candidate)) {
                return candidate;
            }
        }

        // 2. Any dept faculty within hour limits
        List<Faculty> shuffled = new java.util.ArrayList<>(allFaculty);
        Collections.shuffle(shuffled); // randomize among equally-eligible
        for (Faculty f : shuffled) {
            if (isWithinHourLimit(f)) {
                return f;
            }
        }

        // 3. Absolute fallback — pick a random one even if over hours
        if (!allFaculty.isEmpty()) {
            return allFaculty.get(0);
        }
        return null;
    }

    private boolean isWithinHourLimit(Faculty faculty) {
        DesignationConstraint dc = faculty.getDesignationConstraint();
        if (dc == null)
            return true; // no constraint — always eligible
        int maxHours = dc.getMaxTotalHours() != null ? dc.getMaxTotalHours() : Integer.MAX_VALUE;
        long assigned = offeringRepository.getTotalCreditHoursByFaculty(faculty.getId());
        return assigned < maxHours;
    }
}