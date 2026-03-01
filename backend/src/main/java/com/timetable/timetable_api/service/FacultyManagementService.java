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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    @Transactional
    public Faculty createFaculty(FacultyCreationRequest request) {
        validateFacultyRequest(request);

        Department department = resolveDepartment(request.getDepartmentId());
        DesignationConstraint designation = resolveDesignation(request.getDesignation());

        // Find or create the user account — prevents duplicate email errors on
        // re-upload
        User existingUser = userRepository.findByEmail(request.getEmail().trim());
        User savedUser;
        if (existingUser != null) {
            // User already exists: update department linkage if needed
            existingUser.setDepartmentId(department.getId());
            savedUser = userRepository.save(existingUser);
        } else {
            User newUser = new User();
            newUser.setEmail(request.getEmail().trim());
            newUser.setPasswordHash(
                    request.getPassword() != null && !request.getPassword().isEmpty()
                            ? request.getPassword()
                            : "Welcome@123");
            newUser.setRequiresPasswordReset(true);
            newUser.setRole(2); // 2 = FACULTY role
            newUser.setDepartmentId(department.getId());
            savedUser = userRepository.save(newUser);
        }

        Faculty newFaculty = new Faculty();
        newFaculty.setUser(savedUser);
        newFaculty.setDepartment(department);
        newFaculty.setDesignationConstraint(designation);
        newFaculty.setFirstName(request.getFirstName());
        newFaculty.setLastName(request.getLastName());
        newFaculty.setMiddleInitial(request.getMiddleInitial());

        return facultyRepository.save(newFaculty);
    }

    /**
     * Updates an existing faculty member (and related user record).
     * Password is only updated if a non-blank value is provided.
     */
    @Transactional
    public Faculty updateFaculty(Long facultyId, FacultyCreationRequest request) {
        Faculty existing = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found with ID: " + facultyId));

        User linkedUser = existing.getUser();
        if (linkedUser == null) {
            throw new RuntimeException("Faculty record is missing the associated user account.");
        }

        Department department = resolveDepartment(
                request.getDepartmentId() != null ? request.getDepartmentId() : existing.getDepartment().getId());

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            linkedUser.setEmail(request.getEmail().trim());
        }
        // Only update password when the caller explicitly provides a non-blank value
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            linkedUser.setPasswordHash(request.getPassword());
        }
        linkedUser.setDepartmentId(department.getId());
        userRepository.save(linkedUser);

        existing.setDepartment(department);
        if (request.getDesignation() != null && !request.getDesignation().trim().isEmpty()) {
            existing.setDesignationConstraint(resolveDesignation(request.getDesignation()));
        }
        if (request.getFirstName() != null && !request.getFirstName().trim().isEmpty()) {
            existing.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null && !request.getLastName().trim().isEmpty()) {
            existing.setLastName(request.getLastName());
        }
        if (request.getMiddleInitial() != null) {
            existing.setMiddleInitial(request.getMiddleInitial());
        }

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
     * Uses Apache Commons CSV to correctly handle quoted multi-line field values.
     * The entire batch is @Transactional — if any row fails, ALL rows roll back,
     * so a retry on a corrected CSV will never hit duplicate-email conflicts.
     */
    @Transactional
    public List<Faculty> bulkCreateFaculty(InputStream inputStream, Integer defaultDeptId) {
        List<Faculty> createdFaculty = new ArrayList<>();

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .build();

        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                var csvParser = csvFormat.parse(reader)) {

            // Validate required columns exist
            List<String> missingHeaders = new ArrayList<>();
            for (String required : List.of("email", "password", "firstname", "lastname", "designation")) {
                if (!csvParser.getHeaderMap().containsKey(required.toLowerCase(Locale.ROOT))) {
                    missingHeaders.add(required);
                }
            }
            if (!missingHeaders.isEmpty()) {
                throw new RuntimeException("CSV is missing required columns: " + String.join(", ", missingHeaders));
            }

            int rowNumber = 1;
            for (CSVRecord record : csvParser) {
                rowNumber++;
                FacultyCreationRequest request = buildRequestFromRecord(record, rowNumber);
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

    // ── Private helpers ────────────────────────────────────────────────────────

    private FacultyCreationRequest buildRequestFromRecord(CSVRecord record, int rowNumber) {
        FacultyCreationRequest request = new FacultyCreationRequest();
        request.setEmail(getOpt(record, "email"));
        request.setPassword(getOpt(record, "password"));
        request.setFirstName(getOpt(record, "firstname"));
        request.setLastName(getOpt(record, "lastname"));
        request.setDesignation(getOpt(record, "designation"));

        if (record.isMapped("middleinitial")) {
            request.setMiddleInitial(getOpt(record, "middleinitial"));
        }
        String deptIdStr = getOpt(record, "departmentid");
        if (!deptIdStr.isEmpty()) {
            try {
                request.setDepartmentId(Integer.parseInt(deptIdStr));
            } catch (NumberFormatException ex) {
                throw new RuntimeException(
                        "Invalid departmentId on row " + rowNumber + ": \"" + deptIdStr + "\"", ex);
            }
        }
        return request;
    }

    private String getOpt(CSVRecord record, String key) {
        return record.isMapped(key) ? record.get(key).trim() : "";
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
        return designationRepository.findByDesignationIgnoreCase(designationCode.trim())
                .orElseThrow(() -> new RuntimeException(
                        "Designation not found: \"" + designationCode + "\". " +
                                "Valid values are: HOD, Dean, Professor, Associate Professor, " +
                                "Assistant Professor, Senior Assistant Professor, Professor of Practice"));
    }
}