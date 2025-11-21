package com.timetable.timetable_api.service;

import com.timetable.timetable_api.dto.SectionCreationRequest;
import com.timetable.timetable_api.model.Department;
import com.timetable.timetable_api.model.Section;
import com.timetable.timetable_api.repository.DepartmentRepository;
import com.timetable.timetable_api.repository.SectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SectionManagementService {

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private DepartmentRepository departmentRepository; // We need this to link the department

    /**
     * Creates a new Section.
     */
    public Section createSection(SectionCreationRequest request) {
        // 1. Validate and fetch the Department
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Department not found with ID: " + request.getDepartmentId()));

        // 2. Create the new Section
        Section newSection = new Section();
        newSection.setDepartment(department); // Link the department object
        newSection.setName(request.getName());
        newSection.setSemester(request.getSemester());
        newSection.setYear(request.getYear());

        return sectionRepository.save(newSection);
    }

    /**
     * Updates an existing Section.
     */
    public Section updateSection(Long id, SectionCreationRequest request) {
        Section existing = sectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Section not found with ID: " + id));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Department not found with ID: " + request.getDepartmentId()));

        existing.setDepartment(department);
        existing.setName(request.getName());
        existing.setSemester(request.getSemester());
        existing.setYear(request.getYear());

        return sectionRepository.save(existing);
    }

    /**
     * Deletes an existing Section.
     */
    public void deleteSection(Long id) {
        Section existing = sectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Section not found with ID: " + id));
        sectionRepository.delete(existing);
    }

    /**
     * Bulk create sections from CSV input.
     */
    public List<Section> bulkCreateSections(InputStream inputStream) {
        List<Section> createdSections = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new RuntimeException("CSV file is empty.");
            }

            Map<String, Integer> headerIndex = mapHeaderIndexes(headerLine);
            validateRequiredHeaders(headerIndex);

            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] values = splitCsvLine(line);
                SectionCreationRequest request = buildRequestFromRow(values, headerIndex, rowNumber);
                createdSections.add(createSection(request));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSV file: " + e.getMessage(), e);
        }

        return createdSections;
    }

    /**
     * Gets a list of all sections.
     */
    public List<Section> getAllSections() {
        return sectionRepository.findAll();
    }

    /**
     * Gets a single section by its ID.
     */
    public Section getSectionById(Long id) {
        return sectionRepository.findById(id).orElse(null);
    }

    private Map<String, Integer> mapHeaderIndexes(String headerLine) {
        String[] headers = splitCsvLine(headerLine);
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            index.put(headers[i].trim().toLowerCase(Locale.ROOT), i);
        }
        return index;
    }

    private void validateRequiredHeaders(Map<String, Integer> headerIndex) {
        List<String> requiredHeaders = List.of("departmentid", "name", "semester", "year");
        for (String header : requiredHeaders) {
            if (!headerIndex.containsKey(header)) {
                throw new RuntimeException("Missing required CSV header: " + header);
            }
        }
    }

    private SectionCreationRequest buildRequestFromRow(String[] values, Map<String, Integer> headerIndex, int rowNumber) {
        try {
            SectionCreationRequest request = new SectionCreationRequest();
            request.setDepartmentId(Integer.parseInt(getValue(values, headerIndex, "departmentid")));
            request.setName(getValue(values, headerIndex, "name"));
            request.setSemester(Integer.parseInt(getValue(values, headerIndex, "semester")));
            request.setYear(Integer.parseInt(getValue(values, headerIndex, "year")));
            return request;
        } catch (NumberFormatException ex) {
            throw new RuntimeException("Invalid number format on row " + rowNumber + ": " + ex.getMessage(), ex);
        }
    }

    private String getValue(String[] values, Map<String, Integer> headerIndex, String key) {
        Integer idx = headerIndex.get(key);
        if (idx == null || idx >= values.length) {
            return "";
        }
        String raw = values[idx];
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String[] splitCsvLine(String line) {
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    }
}
