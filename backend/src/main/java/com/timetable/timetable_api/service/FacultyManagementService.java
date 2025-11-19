package com.timetable.timetable_api.service;

import com.timetable.timetable_api.dto.FacultyCreationRequest;
import com.timetable.timetable_api.model.Department;
import com.timetable.timetable_api.model.DesignationConstraint;
import com.timetable.timetable_api.model.Faculty;
import com.timetable.timetable_api.model.User;
import com.timetable.timetable_api.repository.DepartmentRepository;
import com.timetable.timetable_api.repository.DesignationConstraintRepository;
import com.timetable.timetable_api.repository.FacultyRepository;
import com.timetable.timetable_api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class FacultyManagementService {

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DesignationConstraintRepository designationRepository;

    /**
     * Creates a new Faculty member along with their User account.
     */
    @Transactional // Ensures if any step fails, everything is rolled back
    public Faculty createFaculty(FacultyCreationRequest request) {

        // 0. Validate required fields are not null
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Password is required");
        }
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new RuntimeException("First name is required");
        }
        if (request.getDateOfJoining() == null) {
            throw new RuntimeException("Date of joining is required");
        }
        if (request.getDateOfBirth() == null) {
            throw new RuntimeException("Date of birth is required");
        }
        if (request.getDepartmentId() == null) {
            throw new RuntimeException("Department ID is required");
        }
        if (request.getDesignation() == null || request.getDesignation().trim().isEmpty()) {
            throw new RuntimeException("Designation is required");
        }

        // 1. Validate dependencies exist
        // Debug: Check if department exists
        Integer deptId = request.getDepartmentId();
        Department department = departmentRepository.findById(deptId)
                .orElseThrow(() -> {
                    // Provide more helpful error message
                    long totalDepartments = departmentRepository.count();
                    return new RuntimeException("Department not found with ID: " + deptId +
                        ". Total departments in database: " + totalDepartments);
                });

        DesignationConstraint designation = designationRepository.findById(request.getDesignation())
                .orElseThrow(() -> new RuntimeException("Designation not found: " + request.getDesignation()));

        // 2. Create and Save the User Account
        User newUser = new User();
        newUser.setEmail(request.getEmail());
        // In a real app, ALWAYS hash passwords (e.g., BCrypt). Storing plain text for now.
        newUser.setPasswordHash(request.getPassword());
        newUser.setRole(2); // 2 = FACULTY role
        User savedUser = userRepository.save(newUser);

        // 3. Create and Save the Faculty Profile
        Faculty newFaculty = new Faculty();
        newFaculty.setUser(savedUser); // Link to the user we just created
        newFaculty.setDepartment(department); // Link to the department
        newFaculty.setDesignationConstraint(designation); // Link to designation rules

        newFaculty.setFirstName(request.getFirstName());
        newFaculty.setLastName(request.getLastName());
        newFaculty.setMiddleInitial(request.getMiddleInitial());
        newFaculty.setDateOfJoining(request.getDateOfJoining());
        newFaculty.setDateOfBirth(request.getDateOfBirth());

        return facultyRepository.save(newFaculty);
    }

    /**
     * Bulk create faculty records from a CSV stream.
     */
    public List<Faculty> bulkCreateFaculty(InputStream inputStream) {
        List<Faculty> createdFaculty = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new RuntimeException("CSV file is empty.");
            }

            Map<String, Integer> headerIndex = mapHeaderIndexes(headerLine);
            validateRequiredHeaders(headerIndex);

            String line;
            int rowNumber = 1; // header
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] values = splitCsvLine(line);
                FacultyCreationRequest request = buildRequestFromRow(values, headerIndex, rowNumber);
                createdFaculty.add(createFaculty(request));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSV file: " + e.getMessage(), e);
        }

        return createdFaculty;
    }

    public List<Faculty> getAllFaculty() {
        return facultyRepository.findAll();
    }

    public Faculty getFacultyById(Long id) {
        return facultyRepository.findById(id).orElse(null);
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
        List<String> requiredHeaders = List.of("email", "password", "firstname", "lastname",
                "dateofjoining", "dateofbirth", "designation", "departmentid");
        for (String header : requiredHeaders) {
            if (!headerIndex.containsKey(header)) {
                throw new RuntimeException("Missing required CSV header: " + header);
            }
        }
    }

    private FacultyCreationRequest buildRequestFromRow(String[] values, Map<String, Integer> headerIndex, int rowNumber) {
        try {
            FacultyCreationRequest request = new FacultyCreationRequest();
            request.setEmail(getValue(values, headerIndex, "email"));
            request.setPassword(getValue(values, headerIndex, "password"));
            request.setFirstName(getValue(values, headerIndex, "firstname"));
            request.setLastName(getValue(values, headerIndex, "lastname"));
            request.setDateOfJoining(LocalDate.parse(getValue(values, headerIndex, "dateofjoining")));
            request.setDateOfBirth(LocalDate.parse(getValue(values, headerIndex, "dateofbirth")));
            request.setDesignation(getValue(values, headerIndex, "designation"));
            request.setDepartmentId(Integer.parseInt(getValue(values, headerIndex, "departmentid")));
            // Optional column
            if (headerIndex.containsKey("middleinitial")) {
                request.setMiddleInitial(getValue(values, headerIndex, "middleinitial"));
            }
            return request;
        } catch (NumberFormatException | DateTimeParseException ex) {
            throw new RuntimeException("Invalid data format on row " + rowNumber + ": " + ex.getMessage(), ex);
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