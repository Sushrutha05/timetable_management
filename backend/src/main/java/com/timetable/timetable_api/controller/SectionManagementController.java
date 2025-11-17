package com.timetable.timetable_api.controller;

import com.timetable.timetable_api.dto.SectionCreationRequest;
import com.timetable.timetable_api.model.Section;
import com.timetable.timetable_api.service.SectionManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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