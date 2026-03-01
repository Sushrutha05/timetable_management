package com.timetable.timetable_api.service;

import com.timetable.timetable_api.dto.CourseCreationRequest;
import com.timetable.timetable_api.model.Course;
import com.timetable.timetable_api.model.Department;
import com.timetable.timetable_api.model.DepartmentCourse;
import com.timetable.timetable_api.model.DepartmentCourseId;
import com.timetable.timetable_api.repository.CourseRepository;
import com.timetable.timetable_api.repository.DepartmentCourseRepository;
import com.timetable.timetable_api.repository.DepartmentRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class CourseManagementService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DepartmentCourseRepository departmentCourseRepository;

    /**
     * Creates a new Course and links it to the department and semester.
     */
    @Transactional
    public DepartmentCourse createCourse(CourseCreationRequest request) {
        if (request.getDepartmentId() == null) {
            throw new RuntimeException("Department ID is required");
        }
        if (request.getSemester() == null) {
            throw new RuntimeException("Semester is required");
        }

        // 1. Find existing course by code, or create a new one
        Course course = courseRepository.findByCourseCode(request.getCourseCode())
                .orElseGet(() -> {
                    Course newCourse = new Course();
                    newCourse.setCourseCode(request.getCourseCode());
                    return newCourse;
                });

        // Always update mutable fields so a re-upload can fix names/hours
        course.setCourseName(request.getCourseName());
        course.setCreditHours(request.getCreditHours());
        course.setCourseType(request.getCourseType() != null ? request.getCourseType() : "THEORY");
        course.setLectureHours(request.getLectureHours() != null ? request.getLectureHours() : 0);
        course.setTutorialHours(request.getTutorialHours() != null ? request.getTutorialHours() : 0);
        course.setPracticalHours(request.getPracticalHours() != null ? request.getPracticalHours() : 0);
        Course savedCourse = courseRepository.save(course);

        // 2. Resolve Department
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Department not found: " + request.getDepartmentId()));

        // 3. Link Department + Course + Semester (skip if already linked)
        DepartmentCourseId linkId = new DepartmentCourseId(department.getId(), savedCourse.getId());
        if (departmentCourseRepository.existsById(linkId)) {
            // Link already exists — return it as-is
            return departmentCourseRepository.findById(linkId).get();
        }

        DepartmentCourse deptCourse = new DepartmentCourse(department, savedCourse, request.getSemester());
        return departmentCourseRepository.save(deptCourse);
    }

    /**
     * Updates an existing course.
     * Note: Access control (checking if admin owns this course) should be done
     * before calling this,
     * or we pass deptId to verify. For now, assuming Controller passes valid scoped
     * ID or we verify link.
     */
    @Transactional
    public DepartmentCourse updateCourse(Long courseId, CourseCreationRequest request) {
        // Find links to check existence and department context if needed
        // For simplicity, we update the Course entity.
        // If we want to update the semester, we need the DepartmentCourse link.
        // Assuming we update the link for the department provided in request.

        Course existingCourse = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found with ID: " + courseId));

        existingCourse.setCourseCode(request.getCourseCode());
        existingCourse.setCourseName(request.getCourseName());
        existingCourse.setCreditHours(request.getCreditHours());
        existingCourse.setCourseType(request.getCourseType());
        if (request.getLectureHours() != null)
            existingCourse.setLectureHours(request.getLectureHours());
        if (request.getTutorialHours() != null)
            existingCourse.setTutorialHours(request.getTutorialHours());
        if (request.getPracticalHours() != null)
            existingCourse.setPracticalHours(request.getPracticalHours());
        courseRepository.save(existingCourse);

        // Update Semester if DepartmentId is provided to identify the link
        if (request.getDepartmentId() != null) {
            DepartmentCourseId linkId = new DepartmentCourseId(request.getDepartmentId(), courseId);
            DepartmentCourse link = departmentCourseRepository.findById(linkId).orElse(null);
            if (link != null && request.getSemester() != null) {
                link.setSemester(request.getSemester());
                return departmentCourseRepository.save(link);
            } else if (link == null) {
                // Determine if we should create a link if it doesn't exist?
                // For now, let's just return what we have (or null if link not found but course
                // updated)
                // But creating a proper return requires a link.
                Department department = departmentRepository.findById(request.getDepartmentId())
                        .orElseThrow(() -> new RuntimeException("Department not found"));
                DepartmentCourse newLink = new DepartmentCourse(department, existingCourse,
                        request.getSemester() != null ? request.getSemester() : 1);
                return departmentCourseRepository.save(newLink);
            }
            return link;
        }

        return null; // Should ideally return the updated object, but we need context
    }

    /**
     * Deletes a course and all its department links.
     */
    @Transactional
    public void deleteCourse(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new RuntimeException("Course not found with ID: " + id);
        }
        // Must delete department_courses links first to satisfy FK constraint
        departmentCourseRepository.deleteByCourseId(id);
        courseRepository.deleteById(id);
    }

    }

    /**
     * Bulk create courses via CSV.
     * Uses Apache Commons CSV to correctly handle quoted multi-line field values.
     */
    @Transactional
    public List<DepartmentCourse> bulkCreateCourses(InputStream inputStream, Integer defaultDeptId) {
        List<DepartmentCourse> createdCourses = new ArrayList<>();
        int rowNumber = 1;

        try {
            Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

            // withFirstRecordAsHeader() maps column names automatically; trim() strips
            // whitespace
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .setIgnoreEmptyLines(true)
                    .build();

            for (CSVRecord record : format.parse(reader)) {
                rowNumber++;

                // Validate required columns exist on first data row
                if (!record.isMapped("coursecode") || !record.isMapped("coursename")
                        || !record.isMapped("credithours")) {
                    throw new RuntimeException(
                            "Missing required CSV header(s). Expected: courseCode, courseName, creditHours");
                }

                CourseCreationRequest request = buildRequestFromRecord(record, rowNumber);

                if (request.getDepartmentId() == null) {
                    request.setDepartmentId(defaultDeptId);
                }

                createdCourses.add(createCourse(request));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSV file: " + e.getMessage(), e);
        }

        return createdCourses;
    }

    /**
     * Gets all courses for a specific department (scoped).
     * Optionally filtered by semester.
     */
    public List<DepartmentCourse> getCoursesByDepartment(Integer deptId, Integer semester) {
        if (semester != null) {
            return departmentCourseRepository.findByDepartmentIdAndSemester(deptId, semester);
        }
        return departmentCourseRepository.findByDepartmentId(deptId);
    }

    // Legacy support or Super Admin support?
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Course getCourseById(Long id) {
        return courseRepository.findById(id).orElse(null);
    }

    private CourseCreationRequest buildRequestFromRecord(CSVRecord record, int rowNumber) {
        CourseCreationRequest request = new CourseCreationRequest();

        request.setCourseCode(record.get("coursecode").trim());
        request.setCourseName(record.get("coursename").trim());

        String creditHoursRaw = record.get("credithours").trim();
        if (creditHoursRaw.isEmpty()) {
            throw new RuntimeException("creditHours is required but was empty on row " + rowNumber);
        }
        request.setCreditHours(parseInt(creditHoursRaw, "creditHours", rowNumber));

        // courseType — default to THEORY if column missing or empty
        String ct = getOpt(record, "coursetype");
        request.setCourseType(ct.isEmpty() ? "THEORY" : ct.toUpperCase(Locale.ROOT));

        // semester — default to 1
        String semRaw = getOpt(record, "semester");
        request.setSemester(semRaw.isEmpty() ? 1 : parseInt(semRaw, "semester", rowNumber));

        // L-T-P hours — default to 0
        String lRaw = getOpt(record, "lecturehours");
        request.setLectureHours(lRaw.isEmpty() ? 0 : parseInt(lRaw, "lectureHours", rowNumber));

        String tRaw = getOpt(record, "tutorialhours");
        request.setTutorialHours(tRaw.isEmpty() ? 0 : parseInt(tRaw, "tutorialHours", rowNumber));

        String pRaw = getOpt(record, "practicalhours");
        request.setPracticalHours(pRaw.isEmpty() ? 0 : parseInt(pRaw, "practicalHours", rowNumber));

        // departmentId — optional, falls back to URL param
        String deptRaw = getOpt(record, "departmentid");
        if (!deptRaw.isEmpty()) {
            request.setDepartmentId(parseInt(deptRaw, "departmentId", rowNumber));
        }

        return request;
    }

    /**
     * Returns the trimmed value for an optional column, or empty string if not
     * present.
     */
    private String getOpt(CSVRecord record, String key) {
        return record.isMapped(key) ? record.get(key).trim() : "";
    }

    /** Parses an int from a string with a friendly error message. */
    private int parseInt(String value, String fieldName, int rowNumber) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw new RuntimeException(
                    "Invalid number for '" + fieldName + "' on row " + rowNumber + " (got: \"" + value + "\")");
        }
    }
}