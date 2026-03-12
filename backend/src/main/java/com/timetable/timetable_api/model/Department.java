package com.timetable.timetable_api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Uses IDENTITY for SERIAL type
    @Column(name = "department_id")
    private Integer id;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    // --- Getters and Setters ---

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}