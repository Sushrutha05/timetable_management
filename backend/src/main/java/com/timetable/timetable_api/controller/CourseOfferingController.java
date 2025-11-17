package com.timetable.timetable_api.controller;

import com.timetable.timetable_api.dto.CourseOfferingRequest;
import com.timetable.timetable_api.model.CourseOffering;
import com.timetable.timetable_api.service.CourseOfferingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/offering") // Base URL for this controller
public class CourseOfferingController {

    @Autowired
    private CourseOfferingService offeringService;

    /**
     * Create a new CourseOffering assignment.
     * Endpoint: POST /api/admin/offering
     */
    @PostMapping
    public ResponseEntity<?> createOffering(@RequestBody CourseOfferingRequest request) {
        try {
            CourseOffering savedOffering = offeringService.createOffering(request);
            return new ResponseEntity<>(savedOffering, HttpStatus.CREATED);
        } catch (Exception e) {
            // Catches any "not found" errors from the service
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Get all CourseOfferings.
     * Endpoint: GET /api/admin/offering
     */
    @GetMapping
    public ResponseEntity<List<CourseOffering>> getAllOfferings() {
        List<CourseOffering> offerings = offeringService.getAllOfferings();
        return new ResponseEntity<>(offerings, HttpStatus.OK);
    }

    /**
     * Delete a CourseOffering by its ID.
     * Endpoint: DELETE /api/admin/offering/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOffering(@PathVariable Long id) {
        try {
            offeringService.deleteOffering(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204 No Content
        } catch (Exception e) {
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}