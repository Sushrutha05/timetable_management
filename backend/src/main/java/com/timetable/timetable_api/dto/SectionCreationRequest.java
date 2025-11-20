package com.timetable.timetable_api.dto;

public class SectionCreationRequest {

    private Integer departmentId; // The ID of the department this section belongs to
    private String name; // e.g., "Section A"
    private Integer semester;
    private Integer year;

    // --- Getters and Setters ---

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
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