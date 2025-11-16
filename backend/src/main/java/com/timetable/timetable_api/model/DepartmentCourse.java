package com.timetable.timetable_api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "department_courses")
public class DepartmentCourse {

    @EmbeddedId // This marks the composite key
    private DepartmentCourseId id;

    // --- Relationships ---

    // This maps the 'department_id' part of our composite key
    @ManyToOne
    @MapsId("departmentId") // "departmentId" must match the field name in DepartmentCourseId
    @JoinColumn(name = "department_id")
    private Department department;

    // This maps the 'course_id' part of our composite key
    @ManyToOne
    @MapsId("courseId") // "courseId" must match the field name in DepartmentCourseId
    @JoinColumn(name = "course_id")
    private Course course;

    // --- Constructors ---
    public DepartmentCourse() {
    }

    public DepartmentCourse(Department department, Course course) {
        this.department = department;
        this.course = course;
        this.id = new DepartmentCourseId(department.getId(), course.getId());
    }

    // --- Getters and Setters ---
    // (Generate them here)

    public DepartmentCourseId getId() {
        return id;
    }

    public void setId(DepartmentCourseId id) {
        this.id = id;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }
}