package com.timetable.timetable_api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "faculty_preferences")
public class FacultyPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preference_id")
    private Long id;

    // --- Relationship 1: Many-to-One with Faculty ---
    // Many preferences can belong to one faculty member.
    @ManyToOne
    @JoinColumn(name = "faculty_id", referencedColumnName = "faculty_id", nullable = false)
    private Faculty faculty;

    // --- Relationship 2: Many-to-One with Course ---
    // Many faculty members can list the same course as a preference.
    @ManyToOne
    @JoinColumn(name = "course_id", referencedColumnName = "course_id", nullable = false)
    private Course course;

    // --- Standard Column ---

    @Column(name = "priority", nullable = false)
    private Integer priority; // e.g., 1, 2, 3

    // --- Getters and Setters ---
    // (Generate them here)

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Faculty getFaculty() {
        return faculty;
    }

    public void setFaculty(Faculty faculty) {
        this.faculty = faculty;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}