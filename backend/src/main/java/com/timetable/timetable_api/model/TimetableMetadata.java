package com.timetable.timetable_api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "timetable_metadata")
public class TimetableMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key", unique = true, nullable = false)
    private String key;

    @Column(name = "value", nullable = false)
    private String value;

    // Default constructor
    public TimetableMetadata() {
    }

    public TimetableMetadata(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
