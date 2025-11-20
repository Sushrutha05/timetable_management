package com.timetable.timetable_api.model;

import jakarta.persistence.*; // Make sure to import from jakarta.persistence

@Entity // Tells Spring this is a database entity
@Table(name = "users") // Maps this class to the "users" table
public class User {

    @Id // Marks this field as the Primary Key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tells Spring that PostgreSQL will auto-generate this ID (using BIGSERIAL)
    @Column(name = "user_id") // Maps this field to the "user_id" column
    private Long id;

    @Column(name = "email", unique = true, nullable = false) // Maps to the "email" column
    private String email;

    @Column(name = "password_hash", nullable = false) // Maps to the "password_hash" column
    private String passwordHash;

    @Column(name = "role", nullable = false) // Maps to the "role" column
    private Integer role; // 1 for ADMIN, 2 for FACULTY

    // --- Getters and Setters ---
    // You need to add getters and setters for all fields so Spring can access them.
    // Your IDE can generate these for you (Right-click -> Generate -> Getters and Setters).

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Integer getRole() {
        return role;
    }

    public void setRole(Integer role) {
        this.role = role;
    }
}