package com.timetable.timetable_api.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.timetable.timetable_api.model.ScheduledClass;
import com.timetable.timetable_api.model.Section;
import com.timetable.timetable_api.repository.ScheduledClassRepository;
import com.timetable.timetable_api.repository.SectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PdfReportService {

    @Autowired
    private ScheduledClassRepository scheduledClassRepository;

    @Autowired
    private SectionRepository sectionRepository;

    private static final List<String> DAYS = List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY",
            "SATURDAY");
    // Standard slots - ideally this should come from TimeSlot repo, but for PDF
    // grid we might standardize
    // Or we can dynamically build columns based on utilized slots.
    // For simplicity, let's assume a standard 9-5 grid or build dynamically.

    public byte[] generateSectionTimetable(Long sectionId) throws DocumentException {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Section not found"));

        List<ScheduledClass> classes = scheduledClassRepository.findByCourseOffering_Section_Id(sectionId);

        return generatePdf(section.getName() + " - Semester " + section.getSemester(), classes);
    }

    public byte[] generateSemesterTimetable(Integer semester) throws DocumentException {
        // Fetch all sections for the semester
        List<Section> sections = sectionRepository.findAll().stream()
                .filter(s -> Objects.equals(s.getSemester(), semester))
                .collect(Collectors.toList());

        if (sections.isEmpty()) {
            throw new RuntimeException("No sections found for semester " + semester);
        }

        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph title = new Paragraph("Timetable - Semester " + semester, titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(Chunk.NEWLINE);

        for (Section section : sections) {
            List<ScheduledClass> classes = scheduledClassRepository.findByCourseOffering_Section_Id(section.getId());
            addSectionTable(document, section.getName(), classes);
            document.add(Chunk.NEWLINE);
        }

        document.close();
        return out.toByteArray();
    }

    private byte[] generatePdf(String titleText, List<ScheduledClass> classes) throws DocumentException {
        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph title = new Paragraph(titleText, titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(Chunk.NEWLINE);

        addSectionTable(document, "Weekly Schedule", classes);

        document.close();
        return out.toByteArray();
    }

    private void addSectionTable(Document document, String subTitle, List<ScheduledClass> classes)
            throws DocumentException {
        Paragraph p = new Paragraph(subTitle, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
        p.setSpacingAfter(10);
        document.add(p);

        // Grid: 7 Columns (Time + 6 Days)
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 2, 3, 3, 3, 3, 3, 3 });

        // Header
        addHeaderCell(table, "Time / Day");
        for (String day : DAYS) {
            addHeaderCell(table, day);
        }

        // Rows: 9:00 to 17:00 (Hourly)
        // Adjust based on your TimeSlots. For now, assuming standard 1-hour slots
        // starting 09:00
        int startHour = 9;
        int endHour = 17;

        for (int h = startHour; h < endHour; h++) {
            LocalTime start = LocalTime.of(h, 0);
            LocalTime end = start.plusHours(1);

            // Time Column
            String timeLabel = start + " - " + end;
            addCell(table, timeLabel, Color.LIGHT_GRAY);

            for (String day : DAYS) {
                // Find class
                ScheduledClass sc = findClass(classes, day, start);
                if (sc != null) {
                    String content = sc.getCourseOffering().getCourse().getCourseCode() + "\n" +
                            sc.getRoom().getRoomNumber() + "\n" +
                            sc.getCourseOffering().getFaculty().getName();
                    addCell(table, content, Color.WHITE);
                } else {
                    addCell(table, "-", Color.WHITE);
                }
            }
        }

        document.add(table);
    }

    private ScheduledClass findClass(List<ScheduledClass> classes, String day, LocalTime start) {
        return classes.stream()
                .filter(c -> c.getDayOfWeek().equalsIgnoreCase(day))
                .filter(c -> !c.getStartTime().isAfter(start) && c.getEndTime().isAfter(start)) // Handles multi-hour
                                                                                                // blocks
                .findFirst()
                .orElse(null);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(Color.GRAY);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 10)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(bgColor);
        cell.setPadding(5);
        table.addCell(cell);
    }
}
