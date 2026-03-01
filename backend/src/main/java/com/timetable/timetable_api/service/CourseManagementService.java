package com.timetable.timetable_api.service;

import com.timetable.timetable_api.dto.CourseCreationRequest;
import com.timetable.timetable_api.model.Course;
import com.timetable.timetable_api.model.Department;
import com.timetable.timetable_api.model.DepartmentCourse;
import com.timetable.timetable_api.model.DepartmentCourseId;
import com.timetable.timetable_api.repository.CourseRepository;
import com.timetable.timetable_api.repository.DepartmentCourseRepository;
import com.timetable.timetable_api.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        // Validation
        if (request.getDepartmentId() == null) {
            throw new RuntimeException("Department ID is required");
        }
        if (request.getSemester() == null) {
            throw new RuntimeException("Semester is required");
        }

        // 1. Create/Save Course
        Course newCourse = new Course();
        newCourse.setCourseCode(request.getCourseCode());
        newCourse.setCourseName(request.getCourseName());
        newCourse.setCreditHours(request.getCreditHours());
        newCourse.setCourseType(request.getCourseType());
        newCourse.setLectureHours(request.getLectureHours() != null ? request.getLectureHours() : 0);
        newCourse.setTutorialHours(request.getTutorialHours() != null ? request.getTutorialHours() : 0);
        newCourse.setPracticalHours(request.getPracticalHours() != null ? request.getPracticalHours() : 0);

        Course savedCourse = courseRepository.save(newCourse);

        // 2. Resolve Department
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Department not found: " + request.getDepartmentId()));

        // 3. Link Department and Course with Semester
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
     * Deletes a course.
     */
    @Transactional
    public void deleteCourse(Long id) {
        // If we delete the course, the link CASCADE might handles it?
        // DepartmentCourse has @ManyToOne to Course.
        // Try deleting course.
        if (courseRepository.existsById(id)) {
            // We might need to delete links first if no cascade
            // departmentCourseRepository.deleteByCourseId(id); // If we add this method
            // For now, try delete course.
            courseRepository.deleteById(id);
        } else {
            throw new RuntimeException("Course not found with ID: " + id);
        }
    }

    /**
     * Bulk create courses via CSV.
     * Requires departmentId to be passed (e.g. from Admin context) or present in
     * CSV.
     */
    @Transactional
    public List<DepartmentCourse> bulkCreateCourses(InputStream inputStream, Integer defaultDeptId) {
        List<DepartmentCourse> createdCourses = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new RuntimeException("CSV file is empty.");
            }

            Map<String, Integer> headerIndex = mapHeaderIndexes(headerLine);
            validateRequiredHeaders(headerIndex);

            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.trim().isEmpty())
                    continue;

                String[] values = splitCsvLine(line);
                CourseCreationRequest request = buildRequestFromRow(values, headerIndex, rowNumber);

                // Use defaultDeptId if CSV doesn't have it (though CSV parser might not extract
                // it if not column)
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

    private Map<String, Integer> mapHeaderIndexes(String headerLine) {
        String[] headers = splitCsvLine(headerLine);
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            index.put(headers[i].trim().toLowerCase(Locale.ROOT), i);
        }
        return index;
    }

    private void validateRequiredHeaders(Map<String, Integer> headerIndex) {
        // Enforce basic headers. 'semester' optional? default to 1?
        List<String> requiredHeaders = List.of("coursecode", "coursename", "credithours");
        for (String header : requiredHeaders) {
            if (!headerIndex.containsKey(header)) {
                throw new RuntimeException("Missing required CSV header: " + header);
            }
        }
    }

    private CourseCreationRequest buildRequestFromRow(String[] values, Map<String, Integer> headerIndex,
            int rowNumber) {
        try {
            CourseCreationRequest request = new CourseCreationRequest();
            request.setCourseCode(getValue(values, headerIndex, "coursecode"));
            request.setCourseName(getValue(values, headerIndex, "coursename"));
            request.setCreditHours(Integer.parseInt(getValue(values, headerIndex, "credithours")));

            if (headerIndex.containsKey("coursetype")) {
                request.setCourseType(getValue(values, headerIndex, "coursetype"));
            }

            if (headerIndex.containsKey("semester")) {
                request.setSemester(Integer.parseInt(getValue(values, headerIndex, "semester")));
            } else {
                request.setSemester(1); // Default
            }

            // If CSV has departmentId
            if (headerIndex.containsKey("departmentid")) {
                request.setDepartmentId(Integer.parseInt(getValue(values, headerIndex, "departmentid")));
            }

            return request;
        } catch (NumberFormatException ex) {
            throw new RuntimeException("Invalid number format on row " + rowNumber + ": " + ex.getMessage(), ex);
        }
    }

    private String getValue(String[] values, Map<String, Integer> headerIndex, String key) {
        Integer idx = headerIndex.get(key);
        if (idx == null || idx >= values.length)
            return "";
        String raw = values[idx];
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String[] splitCsvLine(String line) {
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    }
}