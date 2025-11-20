package com.timetable.timetable_api.controller;

import com.timetable.timetable_api.model.ScheduledClass;
import com.timetable.timetable_api.service.TimetableGenerationService;
import com.timetable.timetable_api.service.TimetableReportService;
import com.timetable.timetable_api.service.TimetableViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/timetable")
public class TimetableGenerationController {

    @Autowired
    private TimetableGenerationService timetableService;

    @Autowired
    private TimetableViewService timetableViewService;

    @Autowired
    private TimetableReportService timetableReportService;

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

    /**
     * Download a section-specific timetable as a file.
     * Endpoint: GET /api/admin/timetable/download/{sectionId}?type=xlsx
     */
    @GetMapping("/download/{sectionId}")
    public ResponseEntity<byte[]> downloadTimetable(@PathVariable Long sectionId,
                                                    @RequestParam(name = "type", defaultValue = "xlsx") String fileType) {
        byte[] fileBytes = timetableReportService.generateSectionTimetableFile(sectionId, fileType);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(String.format("Timetable_Section_%d.xlsx", sectionId))
                .build());
        headers.setContentLength(fileBytes.length);

        return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
    }
}