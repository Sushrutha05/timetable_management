package com.timetable.timetable_api.controller;

import com.timetable.timetable_api.service.PdfReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/export")
@CrossOrigin(origins = "http://localhost:3000")
public class TimetableExportController {

    @Autowired
    private PdfReportService pdfReportService;

    @GetMapping("/pdf/section/{sectionId}")
    public ResponseEntity<byte[]> exportSectionPdf(@PathVariable Long sectionId) {
        try {
            byte[] pdfBytes = pdfReportService.generateSectionTimetable(sectionId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=timetable_section_" + sectionId + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/pdf/semester/{semester}")
    public ResponseEntity<byte[]> exportSemesterPdf(@PathVariable Integer semester) {
        try {
            byte[] pdfBytes = pdfReportService.generateSemesterTimetable(semester);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=timetable_semester_" + semester + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
