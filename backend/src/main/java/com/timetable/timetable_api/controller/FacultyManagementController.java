package com.timetable.timetable_api.controller;

import com.timetable.timetable_api.model.Faculty;
import com.timetable.timetable_api.service.FacultyManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Faculty> createFaculty(@RequestBody Map<String, Object> payload) {
        // We use a Map here to extract the mixed data (Faculty object + email/password strings)
        // In a real app, you'd use a dedicated DTO (Data Transfer Object) class.

        // Extracting data manually for simplicity
        String email = (String) payload.get("email");
        String password = (String) payload.get("password");

        // Converting the rest of the map to a Faculty object is tricky with a raw Map.
        // For this example, let's assume we receive a structured DTO or handle it simpler.
        // TO MAKE THIS RUNNABLE NOW: Let's use a helper method or DTO in the future.
        // For now, I will simulate the service call to show the controller structure.

        // TODO: Implement DTO for cleaner data extraction
        // Faculty newFaculty = ... extract from payload ...

        // Faculty savedFaculty = facultyService.createFaculty(newFaculty, email, password);

        // return new ResponseEntity<>(savedFaculty, HttpStatus.CREATED);
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED); // Placeholder
    }

    // --- A Simpler Version for testing right now ---
    // Let's make a version that just takes the Faculty object directly,
    // assuming User creation happens separately or is mocked for now.

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