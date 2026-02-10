package com.timetable.timetable_api.dto;

public class LoginResponse {
    private Long userId;
    private String email;
    private Integer role;
    private Long facultyId;
    private String firstName;
    private String lastName;

    public LoginResponse(Long userId, String email, Integer role, Long facultyId, String firstName, String lastName) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.facultyId = facultyId;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getRole() {
        return role;
    }

    public void setRole(Integer role) {
        this.role = role;
    }

    public Long getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(Long facultyId) {
        this.facultyId = facultyId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
