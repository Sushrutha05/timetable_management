package com.timetable.timetable_api.dto;

public class DesignationRequest {

    private String designation; // e.g., "Professor"
    private Integer maxLectureHours;
    private Integer maxLabHours;

    // --- Getters and Setters ---

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public Integer getMaxLectureHours() {
        return maxLectureHours;
    }

    public void setMaxLectureHours(Integer maxLectureHours) {
        this.maxLectureHours = maxLectureHours;
    }

    public Integer getMaxLabHours() {
        return maxLabHours;
    }

    public void setMaxLabHours(Integer maxLabHours) {
        this.maxLabHours = maxLabHours;
    }
}