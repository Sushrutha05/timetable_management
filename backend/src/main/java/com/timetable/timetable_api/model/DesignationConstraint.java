package com.timetable.timetable_api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "designation_constraints")
public class DesignationConstraint {

    @Id // This is the Primary Key
    @Column(name = "designation")
    private String designation; // NOT auto-generated!

    @Column(name = "max_lecture_hours", nullable = false)
    private Integer maxLectureHours;

    @Column(name = "max_lab_hours", nullable = false)
    private Integer maxLabHours;

    // --- Getters and Setters ---
    // (Generate them here)

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