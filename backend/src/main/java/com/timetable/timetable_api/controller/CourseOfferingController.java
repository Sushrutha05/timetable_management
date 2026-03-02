package com.timetable.timetable_api.controller;

import com.timetable.timetable_api.dto.CourseOfferingRequest;
import com.timetable.timetable_api.model.CourseOffering;
import com.timetable.timetable_api.service.CourseOfferingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/offering")
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
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    /**
     * One-click auto-generate course offerings for a department.
     * Endpoint: POST /api/admin/offering/auto-generate?deptId=X
     *
     * Skips (course, section) pairs that already have an offering — safe to run
     * multiple times. Returns { "created": N, "skipped": M }
     */
    @PostMapping("/auto-generate")
    public ResponseEntity<?> autoGenerate(@RequestParam Integer deptId) {
        try {
            Map<String, Integer> result = offeringService.autoGenerateOfferings(deptId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    Map.of("error", e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}