package com.timetable.timetable_api.controller;

import com.timetable.timetable_api.model.ScheduledClass;
import com.timetable.timetable_api.service.TimetableGenerationService;
import com.timetable.timetable_api.service.TimetableViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/timetable")
public class TimetableGenerationController {

    @Autowired
    private TimetableGenerationService timetableService;

    @Autowired
    private TimetableViewService timetableViewService;

    /**
     * Generate the master timetable.
     * This will clear the old schedule and create a new one.
     * Endpoint: POST /api/admin/timetable/generate
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateTimetable() {
        try {
            List<ScheduledClass> generatedClasses = timetableService.generateTimetable();
            return new ResponseEntity<>(generatedClasses, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>("Error during generation: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get the full timetable.
     * Endpoint: GET /api/admin/timetable
     */
    @GetMapping
    public ResponseEntity<List<ScheduledClass>> getFullTimetable() {
        List<ScheduledClass> timetable = timetableViewService.getFullTimetable();
        return new ResponseEntity<>(timetable, HttpStatus.OK);
    }
}