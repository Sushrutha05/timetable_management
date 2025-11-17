package com.timetable.timetable_api.controller;

import com.timetable.timetable_api.dto.CourseCreationRequest;
import com.timetable.timetable_api.model.Course;
import com.timetable.timetable_api.service.CourseManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            Course savedCourse = courseService.createCourse(request);
            return new ResponseEntity<>(savedCourse, HttpStatus.CREATED);
        } catch (Exception e) {
            // This will catch database errors, like if you try to add a duplicate course_code
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Get all Courses.
     * Endpoint: GET /api/admin/course
     */
    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        List<Course> courses = courseService.getAllCourses();
        return new ResponseEntity<>(courses, HttpStatus.OK);
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