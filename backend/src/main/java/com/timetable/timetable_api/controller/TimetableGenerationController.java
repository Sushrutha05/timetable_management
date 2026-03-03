package com.timetable.timetable_api.controller;

import com.timetable.timetable_api.dto.UpdateSlotRequest;
import com.timetable.timetable_api.model.ScheduledClass;
import com.timetable.timetable_api.service.TimetableGenerationService;
import com.timetable.timetable_api.model.ScheduledClass;
import com.timetable.timetable_api.service.TimetableGenerationService;
import com.timetable.timetable_api.service.TimetableReportService;
import com.timetable.timetable_api.service.TimetableViewService;
import com.timetable.timetable_api.dto.UpdateSlotRequest;
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

    @Autowired
    private com.timetable.timetable_api.repository.TimetableMetadataRepository metadataRepository;

    /**
     * Generate the timetable for a specific semester parity.
     * Endpoint: POST /api/admin/timetable/generate?parity=EVEN|ODD
     *
     * parity=EVEN → semesters 2, 4, 6, 8
     * parity=ODD → semesters 1, 3, 5, 7
     * (omit parity) → all semesters (legacy)
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateTimetable(
            @RequestParam(name = "parity", required = false) String parity) {
        try {
            List<ScheduledClass> generatedClasses = timetableService.generateTimetable(parity);
            return new ResponseEntity<>(generatedClasses, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>("Error during generation: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get the current timetable publish status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getTimetableStatus() {
        return metadataRepository.findByKey("STATUS")
                .map(m -> new ResponseEntity<>(m.getValue(), HttpStatus.OK))
                .orElse(new ResponseEntity<>("DRAFT", HttpStatus.OK));
    }

    /**
     * Publish the timetable
     */
    @PostMapping("/publish")
    public ResponseEntity<?> publishTimetable() {
        com.timetable.timetable_api.model.TimetableMetadata status = metadataRepository.findByKey("STATUS")
                .orElse(new com.timetable.timetable_api.model.TimetableMetadata("STATUS", "DRAFT"));
        status.setValue("PUBLISHED");
        metadataRepository.save(status);
        return ResponseEntity.ok("Timetable published successfully.");
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
     * Get Timetable for a specific Section
     */
    @GetMapping("/section/{sectionId}")
    public ResponseEntity<List<ScheduledClass>> getTimetableForSection(@PathVariable Long sectionId) {
        List<ScheduledClass> timetable = timetableViewService.getTimetableForSection(sectionId);
        return new ResponseEntity<>(timetable, HttpStatus.OK);
    }

    /**
     * Get Timetable for a specific Faculty
     */
    @GetMapping("/faculty/{facultyId}")
    public ResponseEntity<List<ScheduledClass>> getTimetableForFaculty(@PathVariable Long facultyId) {
        List<ScheduledClass> timetable = timetableViewService.getTimetableForFaculty(facultyId);
        return new ResponseEntity<>(timetable, HttpStatus.OK);
    }

    /**
     * Update a scheduled class slot (drag-and-drop support)
     * Endpoint: POST /api/admin/timetable/update-slot
     */
    @PostMapping("/update-slot")
    public ResponseEntity<?> updateScheduledSlot(@RequestBody UpdateSlotRequest request) {
        try {
            ScheduledClass updated = timetableService.updateScheduledClassSlot(request);
            return new ResponseEntity<>(updated, HttpStatus.OK);
        } catch (RuntimeException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            return new ResponseEntity<>("Unexpected error: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(String.format("Timetable_Section_%d.xlsx", sectionId))
                .build());
        headers.setContentLength(fileBytes.length);

        return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
    }
}