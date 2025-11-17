package com.timetable.timetable_api.dto;

import java.util.List;

// This DTO will receive a list of preferences
public class FacultyPreferenceRequest {

    private List<PreferenceItem> preferences;

    public List<PreferenceItem> getPreferences() {
        return preferences;
    }

    public void setPreferences(List<PreferenceItem> preferences) {
        this.preferences = preferences;
    }

    // A static inner class to represent each item in the list
    public static class PreferenceItem {
        private Long courseId;
        private Integer priority;

        public Long getCourseId() {
            return courseId;
        }
        public void setCourseId(Long courseId) {
            this.courseId = courseId;
        }
        public Integer getPriority() {
            return priority;
        }
        public void setPriority(Integer priority) {
            this.priority = priority;
        }
    }
}