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

import java.util.List;

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

    public List<Faculty> getAllFaculty() {
        return facultyRepository.findAll();
    }

    public Faculty getFacultyById(Long id) {
        return facultyRepository.findById(id).orElse(null);
    }
}