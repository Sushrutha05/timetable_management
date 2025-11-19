package com.timetable.timetable_api.service;

import com.timetable.timetable_api.model.Course;
import com.timetable.timetable_api.model.CourseOffering;
import com.timetable.timetable_api.model.Faculty;
import com.timetable.timetable_api.model.Room;
import com.timetable.timetable_api.model.ScheduledClass;
import com.timetable.timetable_api.model.TimeSlot;
import com.timetable.timetable_api.repository.ScheduledClassRepository;
import com.timetable.timetable_api.repository.TimeSlotRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TimetableReportService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> DEFAULT_DAY_ORDER = List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");

    @Autowired
    private ScheduledClassRepository scheduledClassRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    public byte[] generateSectionTimetableFile(Long sectionId, String fileType) {
        String normalizedType = (fileType == null || fileType.isBlank()) ? "xlsx" : fileType.toLowerCase(Locale.ROOT);
        if (!"xlsx".equals(normalizedType)) {
            normalizedType = "xlsx";
        }

        List<ScheduledClass> scheduledClasses = scheduledClassRepository.findByCourseOffering_Section_Id(sectionId);
        List<TimeSlot> timeSlots = timeSlotRepository.findAllByOrderByDayOfWeekAscStartTimeAsc();
        if (timeSlots.isEmpty()) {
            throw new RuntimeException("No time slots configured. Please define time slots before generating reports.");
        }

        List<String> timeSlotColumns = timeSlots.stream()
                .map(ts -> formatSlot(ts.getStartTime().format(TIME_FORMATTER), ts.getEndTime().format(TIME_FORMATTER)))
                .distinct()
                .collect(Collectors.toList());

        if (timeSlotColumns.isEmpty()) {
            throw new RuntimeException("Unable to determine time slot columns.");
        }

        List<String> dayOrder = buildDayOrder(timeSlots);
        Map<String, Map<String, ScheduledClass>> grid = buildTimetableGrid(scheduledClasses);

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Section " + sectionId);

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle cellStyle = createCellStyle(workbook);

            createHeaderRow(timeSlotColumns, sheet, headerStyle);
            populateTimetableRows(dayOrder, timeSlotColumns, grid, sheet, cellStyle);

            autoSizeColumns(sheet, timeSlotColumns.size());
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to build timetable report: " + e.getMessage(), e);
        }
    }

    private List<String> buildDayOrder(List<TimeSlot> timeSlots) {
        LinkedHashSet<String> orderedDays = new LinkedHashSet<>();
        DEFAULT_DAY_ORDER.forEach(day -> orderedDays.add(day));
        timeSlots.stream()
                .map(ts -> ts.getDayOfWeek() == null ? "" : ts.getDayOfWeek().toUpperCase(Locale.ROOT))
                .forEach(day -> {
                    if (!day.isBlank()) {
                        orderedDays.add(day);
                    }
                });
        return orderedDays.stream().collect(Collectors.toList());
    }

    private Map<String, Map<String, ScheduledClass>> buildTimetableGrid(List<ScheduledClass> scheduledClasses) {
        Map<String, Map<String, ScheduledClass>> grid = new HashMap<>();
        for (ScheduledClass scheduledClass : scheduledClasses) {
            String day = scheduledClass.getDayOfWeek() == null ? "" : scheduledClass.getDayOfWeek().toUpperCase(Locale.ROOT);
            String slotKey = formatSlot(
                    scheduledClass.getStartTime() == null ? "" : scheduledClass.getStartTime().format(TIME_FORMATTER),
                    scheduledClass.getEndTime() == null ? "" : scheduledClass.getEndTime().format(TIME_FORMATTER));

            if (day.isBlank() || slotKey.isBlank()) {
                continue;
            }

            grid.computeIfAbsent(day, key -> new HashMap<>()).put(slotKey, scheduledClass);
        }
        return grid;
    }

    private void createHeaderRow(List<String> timeSlotColumns, Sheet sheet, CellStyle headerStyle) {
        Row header = sheet.createRow(0);
        Cell firstCell = header.createCell(0);
        firstCell.setCellValue("Day / Time");
        firstCell.setCellStyle(headerStyle);

        for (int i = 0; i < timeSlotColumns.size(); i++) {
            Cell cell = header.createCell(i + 1);
            cell.setCellValue(timeSlotColumns.get(i));
            cell.setCellStyle(headerStyle);
        }
    }

    private void populateTimetableRows(List<String> dayOrder,
                                       List<String> timeSlotColumns,
                                       Map<String, Map<String, ScheduledClass>> grid,
                                       Sheet sheet,
                                       CellStyle cellStyle) {
        int rowIndex = 1;
        for (String day : dayOrder) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(capitalize(day));

            Map<String, ScheduledClass> entries = grid.getOrDefault(day, Collections.emptyMap());

            for (int i = 0; i < timeSlotColumns.size(); i++) {
                String slot = timeSlotColumns.get(i);
                ScheduledClass scheduledClass = entries.get(slot);
                Cell cell = row.createCell(i + 1);
                cell.setCellStyle(cellStyle);
                if (scheduledClass != null) {
                    cell.setCellValue(buildCellValue(scheduledClass));
                } else {
                    cell.setCellValue("-");
                }
            }
        }
    }

    private String buildCellValue(ScheduledClass scheduledClass) {
        CourseOffering offering = scheduledClass.getCourseOffering();
        Course course = offering != null ? offering.getCourse() : null;
        Faculty faculty = offering != null ? offering.getFaculty() : null;
        Room room = scheduledClass.getRoom();

        List<String> lines = new ArrayList<>();
        lines.add(course != null ? course.getCourseCode() : "Course: N/A");
        if (room != null) {
            lines.add("Room: " + room.getRoomNumber());
        }
        if (faculty != null) {
            String facultyName = String.format("%s %s",
                    Optional.ofNullable(faculty.getFirstName()).orElse(""),
                    Optional.ofNullable(faculty.getLastName()).orElse("")).trim();
            lines.add("Faculty: " + (facultyName.isEmpty() ? "N/A" : facultyName));
        }
        lines.add(String.format("%s - %s",
                scheduledClass.getStartTime() == null ? "??" : scheduledClass.getStartTime().format(TIME_FORMATTER),
                scheduledClass.getEndTime() == null ? "??" : scheduledClass.getEndTime().format(TIME_FORMATTER)));
        return String.join("\n", lines);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private void autoSizeColumns(Sheet sheet, int timeSlotCount) {
        for (int i = 0; i <= timeSlotCount; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(currentWidth + 1024, 10000));
        }
    }

    private String formatSlot(String start, String end) {
        if (start.isBlank() || end.isBlank()) {
            return "";
        }
        return start + " - " + end;
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}

