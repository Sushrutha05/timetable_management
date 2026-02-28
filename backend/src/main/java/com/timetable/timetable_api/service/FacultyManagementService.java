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
        validateFacultyRequest(request);

        Department department = resolveDepartment(request.getDepartmentId());
        DesignationConstraint designation = resolveDesignation(request.getDesignation());

        // 2. Create and Save the User Account
        User newUser = new User();

        // Enforce Organization Email
        if (request.getEmail() == null || !request.getEmail().toLowerCase().endsWith("@organisation.edu")) {
            throw new RuntimeException("All accounts must use an @organisation.edu email address.");
        }
        newUser.setEmail(request.getEmail());

        // Assign default password and require reset
        // In a real app, ALWAYS hash passwords (e.g., BCrypt). Storing plain text for
        // now,
        // although passwordEncoder is typically available via context. We will set it
        // directly as plain
        // or whatever format the request.getPassword() handles, though we should
        // probably encode it.
        newUser.setPasswordHash(
                request.getPassword() != null && !request.getPassword().isEmpty() ? request.getPassword()
                        : "Welcome@123");
        newUser.setRequiresPasswordReset(true);

        newUser.setRole(2); // 2 = FACULTY role
        // Link to Dept ID for User as well? Faculty user might need it for login logic
        // or scope
        newUser.setDepartmentId(department.getId());
        User savedUser = userRepository.save(newUser);

        // 3. Create and Save the Faculty Profile
        Faculty newFaculty = new Faculty();
        newFaculty.setUser(savedUser); // Link to the user we just created
        newFaculty.setDepartment(department); // Link to the department
        newFaculty.setDesignationConstraint(designation); // Link to designation rules

        newFaculty.setFirstName(request.getFirstName());
        newFaculty.setLastName(request.getLastName());
        newFaculty.setMiddleInitial(request.getMiddleInitial());
        // Removed Date fields

        return facultyRepository.save(newFaculty);
    }

    /**
     * Updates an existing faculty member (and related user record).
     */
    @Transactional
    public Faculty updateFaculty(Long facultyId, FacultyCreationRequest request) {
        validateFacultyRequest(request);

        Faculty existing = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found with ID: " + facultyId));

        User linkedUser = existing.getUser();
        if (linkedUser == null) {
            throw new RuntimeException("Faculty record is missing the associated user account.");
        }

        Department department = resolveDepartment(request.getDepartmentId());
        DesignationConstraint designation = resolveDesignation(request.getDesignation());

        linkedUser.setEmail(request.getEmail());
        linkedUser.setPasswordHash(request.getPassword());
        // Update user dept too?
        linkedUser.setDepartmentId(department.getId());
        userRepository.save(linkedUser);

        existing.setDepartment(department);
        existing.setDesignationConstraint(designation);
        existing.setFirstName(request.getFirstName());
        existing.setLastName(request.getLastName());
        existing.setMiddleInitial(request.getMiddleInitial());
        // Removed Date fields

        return facultyRepository.save(existing);
    }

    /**
     * Deletes a faculty member (User is removed via ON DELETE CASCADE).
     */
    @Transactional
    public void deleteFaculty(Long facultyId) {
        Faculty existing = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found with ID: " + facultyId));
        facultyRepository.delete(existing);
    }

    /**
     * Bulk create faculty records from a CSV stream.
     */
    public List<Faculty> bulkCreateFaculty(InputStream inputStream, Integer defaultDeptId) {
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

                if (request.getDepartmentId() == null) {
                    request.setDepartmentId(defaultDeptId);
                }

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

    public List<Faculty> getFacultyByDepartment(Integer deptId) {
        return facultyRepository.findByDepartmentId(deptId);
    }

    public Faculty getFacultyById(Long id) {
        return facultyRepository.findById(id).orElse(null);
    }

    private void validateFacultyRequest(FacultyCreationRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Password is required");
        }
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new RuntimeException("First name is required");
        }
        // Removed Date validations
        if (request.getDepartmentId() == null) {
            throw new RuntimeException("Department ID is required");
        }
        if (request.getDesignation() == null || request.getDesignation().trim().isEmpty()) {
            throw new RuntimeException("Designation is required");
        }
    }

    private Department resolveDepartment(Integer deptId) {
        return departmentRepository.findById(deptId)
                .orElseThrow(() -> {
                    long totalDepartments = departmentRepository.count();
                    return new RuntimeException("Department not found with ID: " + deptId +
                            ". Total departments in database: " + totalDepartments);
                });
    }

    private DesignationConstraint resolveDesignation(String designationCode) {
        return designationRepository.findById(designationCode)
                .orElseThrow(() -> new RuntimeException("Designation not found: " + designationCode));
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
        // Removed dateofjoining, dateofbirth
        List<String> requiredHeaders = List.of("email", "password", "firstname", "lastname",
                "designation"); // Department ID might be from context or CSV
        for (String header : requiredHeaders) {
            if (!headerIndex.containsKey(header)) {
                throw new RuntimeException("Missing required CSV header: " + header);
            }
        }
    }

    private FacultyCreationRequest buildRequestFromRow(String[] values, Map<String, Integer> headerIndex,
            int rowNumber) {
        try {
            FacultyCreationRequest request = new FacultyCreationRequest();
            request.setEmail(getValue(values, headerIndex, "email"));
            request.setPassword(getValue(values, headerIndex, "password"));
            request.setFirstName(getValue(values, headerIndex, "firstname"));
            request.setLastName(getValue(values, headerIndex, "lastname"));
            // Removed Dates
            request.setDesignation(getValue(values, headerIndex, "designation"));

            if (headerIndex.containsKey("departmentid")) {
                request.setDepartmentId(Integer.parseInt(getValue(values, headerIndex, "departmentid")));
            }

            // Optional column
            if (headerIndex.containsKey("middleinitial")) {
                request.setMiddleInitial(getValue(values, headerIndex, "middleinitial"));
            }
            return request;
        } catch (NumberFormatException ex) {
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