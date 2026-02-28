package com.timetable.timetable_api.controller;

import com.timetable.timetable_api.dto.LoginRequest;
import com.timetable.timetable_api.dto.LoginResponse;
import com.timetable.timetable_api.model.Faculty;
import com.timetable.timetable_api.model.User;
import com.timetable.timetable_api.repository.FacultyRepository;
import com.timetable.timetable_api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail());

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        }

        Long facultyId = null;
        Integer deptId = null;
        String firstName = null;
        String lastName = null;

        // If user is potentially faculty (Role 2 usually, need to check constants or
        // setup)
        // Or blindly check if faculty record exists
        Faculty faculty = facultyRepository.findByUserId(user.getId());
        if (faculty != null) {
            facultyId = faculty.getId();
            deptId = faculty.getDepartment().getId();
            firstName = faculty.getFirstName();
            lastName = faculty.getLastName();
        }

        // For non-faculty (e.g., Admin), try to get deptId from User entity
        if (deptId == null && user.getDepartmentId() != null) {
            deptId = user.getDepartmentId();
        }

        LoginResponse response = new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                facultyId,
                deptId,
                firstName,
                lastName,
                user.getRequiresPasswordReset());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody com.timetable.timetable_api.dto.PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail());

        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid user");
        }

        // Verify the old password matches the common default or whatever is currently
        // set
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Incorrect current password.");
        }

        // Update to new password and set requiresReset to false
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setRequiresPasswordReset(false);
        userRepository.save(user);

        return ResponseEntity.ok("Password reset successfully.");
    }
}
