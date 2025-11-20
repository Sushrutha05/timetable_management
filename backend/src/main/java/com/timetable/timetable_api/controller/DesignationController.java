package com.timetable.timetable_api.controller;

import com.timetable.timetable_api.dto.DesignationRequest;
import com.timetable.timetable_api.model.DesignationConstraint;
import com.timetable.timetable_api.service.DesignationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/designation") // Base URL for this controller
public class DesignationController {

    @Autowired
    private DesignationService designationService;

    /**
     * Create or Update a Designation Constraint.
     * Endpoint: POST /api/admin/designation
     */
    @PostMapping
    public ResponseEntity<DesignationConstraint> createOrUpdateDesignation(@RequestBody DesignationRequest request) {
        DesignationConstraint savedConstraint = designationService.createOrUpdateDesignation(request);
        return new ResponseEntity<>(savedConstraint, HttpStatus.OK); // OK, since it can be an update
    }

    /**
     * Get all Designation Constraints.
     * Endpoint: GET /api/admin/designation
     */
    @GetMapping
    public ResponseEntity<List<DesignationConstraint>> getAllDesignations() {
        List<DesignationConstraint> constraints = designationService.getAllDesignations();
        return new ResponseEntity<>(constraints, HttpStatus.OK);
    }

    /**
     * Get a single Designation Constraint by its name.
     * Endpoint: GET /api/admin/designation/{name}
     */
    @GetMapping("/{name}")
    public ResponseEntity<DesignationConstraint> getDesignationById(@PathVariable String name) {
        DesignationConstraint constraint = designationService.getDesignationById(name);
        if (constraint != null) {
            return new ResponseEntity<>(constraint, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Delete a Designation Constraint.
     * Endpoint: DELETE /api/admin/designation/{name}
     */
    @DeleteMapping("/{name}")
    public ResponseEntity<Void> deleteDesignation(@PathVariable String name) {
        try {
            designationService.deleteDesignation(name);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204 No Content
        } catch (Exception e) {
            // e.g., if you try to delete a designation that's in use by a faculty
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}