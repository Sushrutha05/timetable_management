package com.timetable.timetable_api.dto;

public class LoginResponse {
    private Long userId;
    private String email;
    private Integer role;
    private Long facultyId;
    private Integer deptId;
    private String firstName;
    private String lastName;
    private Boolean requiresPasswordReset;

    public LoginResponse(Long userId, String email, Integer role, Long facultyId, Integer deptId, String firstName,
            String lastName, Boolean requiresPasswordReset) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.facultyId = facultyId;
        this.deptId = deptId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.requiresPasswordReset = requiresPasswordReset;
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

    public Integer getDeptId() {
        return deptId;
    }

    public void setDeptId(Integer deptId) {
        this.deptId = deptId;
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

    public Boolean getRequiresPasswordReset() {
        return requiresPasswordReset;
    }

    public void setRequiresPasswordReset(Boolean requiresPasswordReset) {
        this.requiresPasswordReset = requiresPasswordReset;
    }
}
