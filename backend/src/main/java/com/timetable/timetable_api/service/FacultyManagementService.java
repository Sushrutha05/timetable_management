package com.timetable.timetable_api.service;

import com.timetable.timetable_api.model.Faculty;
import com.timetable.timetable_api.model.User;
import com.timetable.timetable_api.repository.FacultyRepository;
import com.timetable.timetable_api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service // Declares this as a Spring Service
public class FacultyManagementService {

    // --- Dependency Injection ---
    // We ask Spring to "inject" the repositories we need.

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private UserRepository userRepository;

    // You would also inject other repositories as needed, e.g., DepartmentRepository

    /**
     * Business logic for creating a new faculty member.
     * This is more complex than just saving a faculty.
     * We need to create a User, save it, then link it to a new Faculty, and save that.
     * (We'll simplify password hashing for now).
     */
    public Faculty createFaculty(Faculty facultyData, String email, String password) {

        // 1. Create the User entity
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setPasswordHash(password); // In a real app, you'd HASH this password
        newUser.setRole(2); // 2 = FACULTY

        // 2. Save the new User to the DB
        User savedUser = userRepository.save(newUser);

        // 3. Link the saved User to the new Faculty profile
        facultyData.setUser(savedUser);

        // 4. Save the new Faculty profile
        // This will now have the user_id and department_id
        return facultyRepository.save(facultyData);
    }

    /**
     * Gets a list of all faculty members.
     * This is a simple pass-through.
     */
    public List<Faculty> getAllFaculty() {
        return facultyRepository.findAll();
    }

    /**
     * Gets a single faculty member by their ID.
     */
    public Faculty getFacultyById(Long id) {
        // findById returns an Optional, so we use .orElse(null) for simplicity
        return facultyRepository.findById(id).orElse(null);
    }

    // You would add other methods here for the Admin:
    // - public Faculty updateFaculty(Long id, Faculty facultyDetails) { ... }
    // - public void deleteFaculty(Long id) { ... }
}