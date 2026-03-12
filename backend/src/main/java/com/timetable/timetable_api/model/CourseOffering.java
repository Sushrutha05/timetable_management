package com.timetable.timetable_api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "course_offerings")
public class CourseOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "offering_id")
    private Long id;

    // --- Relationship 1: Many-to-One with Course ---
    // Many offerings (e.g., to Section A, Section B) can be for the same course.
    @ManyToOne
    @JoinColumn(name = "course_id", referencedColumnName = "course_id", nullable = false)
    private Course course;

    // --- Relationship 2: Many-to-One with Faculty ---
    // One faculty member can teach many different offerings.
    @ManyToOne
    @JoinColumn(name = "faculty_id", referencedColumnName = "faculty_id", nullable = false)
    private Faculty faculty;

    // --- Relationship 3: Many-to-One with Section ---
    // One section can have many different offerings (e.g., Math, History).
    @ManyToOne
    @JoinColumn(name = "section_id", referencedColumnName = "section_id", nullable = false)
    private Section section;

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Faculty getFaculty() {
        return faculty;
    }

    public void setFaculty(Faculty faculty) {
        this.faculty = faculty;
    }

    public Section getSection() {
        return section;
    }

    public void setSection(Section section) {
        this.section = section;
    }
}