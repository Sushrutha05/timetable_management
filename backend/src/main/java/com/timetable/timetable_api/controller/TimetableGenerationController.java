package com.timetable.timetable_api.controller;

import com.timetable.timetable_api.model.ScheduledClass;
import com.timetable.timetable_api.service.TimetableGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/timetable")
public class TimetableGenerationController {

    @Autowired
    private TimetableGenerationService timetableService;

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
}