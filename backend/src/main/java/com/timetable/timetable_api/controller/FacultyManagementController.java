package com.timetable.timetable_api.controller;

import com.timetable.timetable_api.dto.FacultyCreationRequest;
import com.timetable.timetable_api.model.Faculty;
import com.timetable.timetable_api.service.FacultyManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController // 1. Tells Spring this class handles REST requests
@RequestMapping("/api/admin/faculty") // 2. Sets the base URL for all methods in this class
public class FacultyManagementController {

    @Autowired
    private FacultyManagementService facultyService;

    /**
     * Create a new Faculty member.
     * Endpoint: POST /api/admin/faculty
     * Body: JSON object with faculty details + email + password
     */
    @PostMapping
    public ResponseEntity<?> createFaculty(@RequestBody FacultyCreationRequest request) {
        try {
            Faculty savedFaculty = facultyService.createFaculty(request);
            return new ResponseEntity<>(savedFaculty, HttpStatus.CREATED);
        } catch (DataIntegrityViolationException e) {
            // Handle unique constraint violations (e.g., duplicate email)
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("email")) {
                errorMessage = "Email already exists. Please use a different email address.";
            } else if (errorMessage != null && errorMessage.contains("unique")) {
                errorMessage = "A record with this information already exists.";
            } else {
                errorMessage = "Data integrity violation: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            }
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", errorMessage);
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            // Handle business logic exceptions (e.g., Department not found, Designation not found)
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            // Handle any other unexpected exceptions
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "An error occurred while creating the faculty: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Bulk upload faculty via CSV.
     * Endpoint: POST /api/admin/faculty/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<?> bulkUploadFaculty(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Uploaded file is empty."));
        }

        try {
            List<Faculty> createdFaculty = facultyService.bulkCreateFaculty(file.getInputStream());
            return new ResponseEntity<>(createdFaculty, HttpStatus.CREATED);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to read uploaded file: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Bulk upload failed: " + e.getMessage()));
        }
    }

    /**
     * Get all faculty members.
     * Endpoint: GET /api/admin/faculty
     */
    @GetMapping
    public ResponseEntity<List<Faculty>> getAllFaculty() {
        List<Faculty> facultyList = facultyService.getAllFaculty();
        return new ResponseEntity<>(facultyList, HttpStatus.OK);
    }

    /**
     * Get a single faculty member by ID.
     * Endpoint: GET /api/admin/faculty/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Faculty> getFacultyById(@PathVariable Long id) {
        Faculty faculty = facultyService.getFacultyById(id);
        if (faculty != null) {
            return new ResponseEntity<>(faculty, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}