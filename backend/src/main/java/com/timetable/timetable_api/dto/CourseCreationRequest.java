package com.timetable.timetable_api.dto;

public class CourseCreationRequest {

    private String courseCode;
    private String courseName;
    private Integer creditHours;

    // --- Getters and Setters ---
    // (You can generate these in your IDE)

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
}