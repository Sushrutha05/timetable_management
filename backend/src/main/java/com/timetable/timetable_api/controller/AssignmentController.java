package com.timetable.timetable_api.controller;

import com.timetable.timetable_api.service.TimetableAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/assignment")
public class AssignmentController {

    @Autowired
    private TimetableAssignmentService timetableAssignmentService;

    /**
     * Get aggregated assignment data for a given section.
     * Endpoint: GET /api/admin/assignment/data/{sectionId}
     */
    @GetMapping("/data/{sectionId}")
    public ResponseEntity<?> getAssignmentData(@PathVariable Long sectionId) {
        try {
            Map<String, Object> data = timetableAssignmentService.getAssignmentData(sectionId);
            return new ResponseEntity<>(data, HttpStatus.OK);
        } catch (RuntimeException ex) {
            return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }
}
