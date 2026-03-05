package com.timetable.timetable_api.model;

import jakarta.persistence.*;
import java.time.LocalTime; // Import for the TIME type

@Entity
@Table(name = "scheduled_classes")
public class ScheduledClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_id")
    private Long id;

    // --- Relationship 1: Many-to-One with CourseOffering ---
    // Many scheduled classes can come from one course offering
    // (e.g., if a 3-credit course is split into 3 classes per week).
    // Or, this will be a 1-to-1 if you schedule one "block" per offering.
    @ManyToOne
    @JoinColumn(name = "offering_id", referencedColumnName = "offering_id", nullable = false)
    private CourseOffering courseOffering;

    // --- Relationship 2: Many-to-One with Room ---
    // One room can host many different scheduled classes.
    @ManyToOne
    @JoinColumn(name = "room_id", referencedColumnName = "room_id", nullable = false)
    private Room room;

    // --- Standard Columns ---

    @Column(name = "day_of_week", nullable = false)
    private String dayOfWeek; // e.g., "MONDAY"

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime; // Use LocalTime for TIME

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime; // Use LocalTime for TIME

    // --- Relationship 3: Many-to-One with Faculty (Substitute) ---
    // If a lab or specific class is taken by a substitute faculty, store it here.
    @ManyToOne
    @JoinColumn(name = "assigned_faculty_id", referencedColumnName = "faculty_id", nullable = true)
    private Faculty assignedFaculty;

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CourseOffering getCourseOffering() {
        return courseOffering;
    }

    public void setCourseOffering(CourseOffering courseOffering) {
        this.courseOffering = courseOffering;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public Faculty getAssignedFaculty() {
        return assignedFaculty;
    }

    public void setAssignedFaculty(Faculty assignedFaculty) {
        this.assignedFaculty = assignedFaculty;
    }
}