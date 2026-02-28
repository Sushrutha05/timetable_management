package com.timetable.timetable_api.controller;

import com.timetable.timetable_api.dto.CourseCreationRequest;
import com.timetable.timetable_api.model.Course;
import com.timetable.timetable_api.service.CourseManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/course") // Base URL for this controller
public class CourseManagementController {

    @Autowired
    private CourseManagementService courseService;

    /**
     * Create a new Course.
     * Endpoint: POST /api/admin/course
     */
    @PostMapping
    public ResponseEntity<?> createCourse(@RequestBody CourseCreationRequest request) {
        try {
            // Service returns DepartmentCourse
            Object savedCourse = courseService.createCourse(request);
            return new ResponseEntity<>(savedCourse, HttpStatus.CREATED);
        } catch (Exception e) {
            // This will catch database errors, like if you try to add a duplicate
            // course_code
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Update a Course.
     * Endpoint: PUT /api/admin/course/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCourse(@PathVariable Long id, @RequestBody CourseCreationRequest request) {
        try {
            // Service returns DepartmentCourse
            Object updatedCourse = courseService.updateCourse(id, request);
            return new ResponseEntity<>(updatedCourse, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Delete a Course.
     * Endpoint: DELETE /api/admin/course/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCourse(@PathVariable Long id) {
        try {
            courseService.deleteCourse(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Bulk upload courses via CSV.
     * Endpoint: POST /api/admin/course/upload
     * Query Param: deptId (Required)
     */
    @PostMapping("/upload")
    public ResponseEntity<?> bulkUploadCourses(@RequestParam("file") MultipartFile file,
            @RequestParam("deptId") Integer deptId) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Uploaded file is empty."));
        }
        if (deptId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Department ID is required."));
        }

        try {
            // Service returns List<DepartmentCourse>
            List<?> createdCourses = courseService.bulkCreateCourses(file.getInputStream(), deptId);
            return new ResponseEntity<>(createdCourses, HttpStatus.CREATED);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to read uploaded file: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Bulk upload failed: " + e.getMessage()));
        }
    }

    /**
     * Get all Courses for a Department.
     * Endpoint: GET /api/admin/course
     * Query Params: deptId (Required), semester (Optional)
     */
    @GetMapping
    public ResponseEntity<?> getAllCourses(@RequestParam(required = false) Integer deptId,
            @RequestParam(required = false) Integer semester) {
        if (deptId != null) {
            List<?> courses = courseService.getCoursesByDepartment(deptId, semester);
            return new ResponseEntity<>(courses, HttpStatus.OK);
        } else {
            // Fallback to all courses (Legacy/SuperAdmin)
            return new ResponseEntity<>(courseService.getAllCourses(), HttpStatus.OK);
        }
    }

    /**
     * Get a single Course by ID.
     * Endpoint: GET /api/admin/course/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourseById(@PathVariable Long id) {
        Course course = courseService.getCourseById(id);
        if (course != null) {
            return new ResponseEntity<>(course, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
