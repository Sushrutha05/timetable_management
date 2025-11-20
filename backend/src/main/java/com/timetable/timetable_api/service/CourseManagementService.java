package com.timetable.timetable_api.service;

import com.timetable.timetable_api.dto.CourseCreationRequest;
import com.timetable.timetable_api.model.Course;
import com.timetable.timetable_api.repository.CourseRepository;
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
public class CourseManagementService {

    @Autowired
    private CourseRepository courseRepository;

    /**
     * Creates a new Course.
     */
    public Course createCourse(CourseCreationRequest request) {
        // You could add validation here, e.g., check if courseCode already exists

        Course newCourse = new Course();
        newCourse.setCourseCode(request.getCourseCode());
        newCourse.setCourseName(request.getCourseName());
        newCourse.setCreditHours(request.getCreditHours());

        return courseRepository.save(newCourse);
    }

    /**
     * Bulk create courses via CSV upload.
     */
    public List<Course> bulkCreateCourses(InputStream inputStream) {
        List<Course> createdCourses = new ArrayList<>();

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
                CourseCreationRequest request = buildRequestFromRow(values, headerIndex, rowNumber);
                createdCourses.add(createCourse(request));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSV file: " + e.getMessage(), e);
        }

        return createdCourses;
    }

    /**
     * Gets a list of all courses.
     */
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    /**
     * Gets a single course by its ID.
     */
    public Course getCourseById(Long id) {
        return courseRepository.findById(id).orElse(null);
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
        List<String> requiredHeaders = List.of("coursecode", "coursename", "credithours");
        for (String header : requiredHeaders) {
            if (!headerIndex.containsKey(header)) {
                throw new RuntimeException("Missing required CSV header: " + header);
            }
        }
    }

    private CourseCreationRequest buildRequestFromRow(String[] values, Map<String, Integer> headerIndex, int rowNumber) {
        try {
            CourseCreationRequest request = new CourseCreationRequest();
            request.setCourseCode(getValue(values, headerIndex, "coursecode"));
            request.setCourseName(getValue(values, headerIndex, "coursename"));
            request.setCreditHours(Integer.parseInt(getValue(values, headerIndex, "credithours")));
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