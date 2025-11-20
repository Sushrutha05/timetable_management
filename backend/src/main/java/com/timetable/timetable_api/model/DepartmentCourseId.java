package com.timetable.timetable_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable // Declares this as a component of another entity
public class DepartmentCourseId implements Serializable {

    @Column(name = "department_id")
    private Integer departmentId;

    @Column(name = "course_id")
    private Long courseId;

    // --- Constructors ---
    // A no-argument constructor is required by JPA
    public DepartmentCourseId() {
    }

    public DepartmentCourseId(Integer departmentId, Long courseId) {
        this.departmentId = departmentId;
        this.courseId = courseId;
    }

    // --- Getters and Setters ---
    // (Generate them here)

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }


    // --- equals() and hashCode() ---
    // These are ESSENTIAL for composite keys.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DepartmentCourseId that = (DepartmentCourseId) o;
        return Objects.equals(departmentId, that.departmentId) &&
                Objects.equals(courseId, that.courseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(departmentId, courseId);
    }
}