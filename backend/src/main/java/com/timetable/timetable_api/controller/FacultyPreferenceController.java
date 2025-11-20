package com.timetable.timetable_api.controller;

import com.timetable.timetable_api.dto.FacultyPreferenceRequest;
import com.timetable.timetable_api.model.FacultyPreference;
import com.timetable.timetable_api.model.ScheduledClass;
import com.timetable.timetable_api.service.FacultyPreferenceService;
import com.timetable.timetable_api.service.TimetableViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faculty") // This is the base URL for faculty-specific tasks
public class FacultyPreferenceController {

    @Autowired
    private FacultyPreferenceService preferenceService;

    @Autowired
    private TimetableViewService timetableViewService;

    /**
     * Get all preferences for a specific faculty member.
     * Endpoint: GET /api/faculty/{facultyId}/preferences
     */
    @GetMapping("/{facultyId}/preferences")
    public ResponseEntity<List<FacultyPreference>> getPreferences(@PathVariable Long facultyId) {
        List<FacultyPreference> preferences = preferenceService.getPreferencesByFaculty(facultyId);
        return new ResponseEntity<>(preferences, HttpStatus.OK);
    }

    /**
     * Set (or overwrite) the preferences for a specific faculty.
     * Endpoint: POST /api/faculty/{facultyId}/preferences
     */
    @PostMapping("/{facultyId}/preferences")
    public ResponseEntity<?> setPreferences(@PathVariable Long facultyId, @RequestBody FacultyPreferenceRequest request) {
        try {
            List<FacultyPreference> savedPreferences = preferenceService.setPreferences(facultyId, request);
            return new ResponseEntity<>(savedPreferences, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Get the timetable for a specific faculty member.
     * Endpoint: GET /api/faculty/{facultyId}/timetable
     */
    @GetMapping("/{facultyId}/timetable")
    public ResponseEntity<List<ScheduledClass>> getTimetable(@PathVariable Long facultyId) {
        List<ScheduledClass> timetable = timetableViewService.getTimetableForFaculty(facultyId);
        return new ResponseEntity<>(timetable, HttpStatus.OK);
    }
}