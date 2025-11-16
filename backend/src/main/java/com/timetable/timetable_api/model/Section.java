package com.timetable.timetable_api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "sections")
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "section_id")
    private Long id;

    // --- Relationship: Many-to-One with Department ---
    // Many sections (e.g., "Section A", "Section B") can belong to one department.
    @ManyToOne
    @JoinColumn(name = "department_id", referencedColumnName = "department_id", nullable = false)
    private Department department;

    // --- Standard Columns ---

    @Column(name = "name", nullable = false)
    private String name; // e.g., "Section A"

    @Column(name = "semester", nullable = false)
    private Integer semester; // e.g., 3, 5, 7

    @Column(name = "year", nullable = false)
    private Integer year; // e.g., 2025

    // --- Getters and Setters ---
    // (Generate them here)

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }
}