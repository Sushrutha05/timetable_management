package com.timetable.timetable_api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Long id;

    @Column(name = "course_code", unique = true, nullable = false)
    private String courseCode;

    @Column(name = "course_name", unique = true, nullable = false)
    private String courseName;

    @Column(name = "credit_hours", nullable = false)
    private Integer creditHours;

    // --- THIS IS THE FIX ---
    // We are telling Hibernate the exact SQL to use for this column,
    // which includes a DEFAULT value.
    @Column(name = "course_type")
    private String courseType;

    @Column(name = "lecture_hours")
    private Integer lectureHours = 0;

    @Column(name = "tutorial_hours")
    private Integer tutorialHours = 0;

    @Column(name = "practical_hours")
    private Integer practicalHours = 0;

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public Integer getCreditHours() {
        return creditHours;
    }

    public void setCreditHours(Integer creditHours) {
        this.creditHours = creditHours;
    }

    // --- ADD GETTER/SETTER for courseType ---
    public String getCourseType() {
        return courseType;
    }

    public void setCourseType(String courseType) {
        this.courseType = courseType;
    }

    public Integer getLectureHours() {
        return lectureHours;
    }

    public void setLectureHours(Integer lectureHours) {
        this.lectureHours = lectureHours;
    }

    public Integer getTutorialHours() {
        return tutorialHours;
    }

    public void setTutorialHours(Integer tutorialHours) {
        this.tutorialHours = tutorialHours;
    }

    public Integer getPracticalHours() {
        return practicalHours;
    }

    public void setPracticalHours(Integer practicalHours) {
        this.practicalHours = practicalHours;
    }
}