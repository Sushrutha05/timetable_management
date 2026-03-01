package com.timetable.timetable_api.dto;

public class CourseCreationRequest {

    private String courseCode;
    private String courseName;
    private Integer creditHours;
    private String courseType;
    private Integer semester;
    private Integer departmentId;
    private Integer lectureHours;
    private Integer tutorialHours;
    private Integer practicalHours;

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

    public String getCourseType() {
        return courseType;
    }

    public void setCourseType(String courseType) {
        this.courseType = courseType;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
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