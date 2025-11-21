package com.timetable.timetable_api.controller;

import com.timetable.timetable_api.dto.SectionCreationRequest;
import com.timetable.timetable_api.model.Section;
import com.timetable.timetable_api.service.SectionManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/section") // Base URL for this controller
public class SectionManagementController {

    @Autowired
    private SectionManagementService sectionService;

    /**
     * Create a new Section.
     * Endpoint: POST /api/admin/section
     */
    @PostMapping
    public ResponseEntity<?> createSection(@RequestBody SectionCreationRequest request) {
        try {
            Section savedSection = sectionService.createSection(request);
            return new ResponseEntity<>(savedSection, HttpStatus.CREATED);
        } catch (Exception e) {
            // Catches the "Department not found" error
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Update a Section.
     * Endpoint: PUT /api/admin/section/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSection(@PathVariable Long id, @RequestBody SectionCreationRequest request) {
        try {
            Section updatedSection = sectionService.updateSection(id, request);
            return new ResponseEntity<>(updatedSection, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Delete a Section.
     * Endpoint: DELETE /api/admin/section/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSection(@PathVariable Long id) {
        try {
            sectionService.deleteSection(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Bulk upload sections via CSV.
     * Endpoint: POST /api/admin/section/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<?> bulkUploadSections(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Uploaded file is empty."));
        }

        try {
            List<Section> createdSections = sectionService.bulkCreateSections(file.getInputStream());
            return new ResponseEntity<>(createdSections, HttpStatus.CREATED);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to read uploaded file: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Bulk upload failed: " + e.getMessage()));
        }
    }

    /**
     * Get all Sections.
     * Endpoint: GET /api/admin/section
     */
    @GetMapping
    public ResponseEntity<List<Section>> getAllSections() {
        List<Section> sections = sectionService.getAllSections();
        return new ResponseEntity<>(sections, HttpStatus.OK);
    }

    /**
     * Get a single Section by ID.
     * Endpoint: GET /api/admin/section/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Section> getSectionById(@PathVariable Long id) {
        Section section = sectionService.getSectionById(id);
        if (section != null) {
            return new ResponseEntity<>(section, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
