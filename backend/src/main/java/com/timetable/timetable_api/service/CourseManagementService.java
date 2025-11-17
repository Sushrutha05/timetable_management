package com.timetable.timetable_api.service;

import com.timetable.timetable_api.dto.CourseCreationRequest;
import com.timetable.timetable_api.model.Course;
import com.timetable.timetable_api.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseManagementService {

    @Autowired
    private CourseRepository courseRepository;

    /**
     * Creates a new Course.
     */
    public Course createCourse(CourseCreationRequest request) {
        // You could add validation here, e.g., check if courseCode already exists

        Course newCourse = new Course();
        newCourse.setCourseCode(request.getCourseCode());
        newCourse.setCourseName(request.getCourseName());
        newCourse.setCreditHours(request.getCreditHours());

        return courseRepository.save(newCourse);
    }

    /**
     * Gets a list of all courses.
     */
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    /**
     * Gets a single course by its ID.
     */
    public Course getCourseById(Long id) {
        return courseRepository.findById(id).orElse(null);
    }
}