package com.timetable.timetable_api.repository;

import com.timetable.timetable_api.model.User; // Import your User model
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository // Declares this as a Spring-managed repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
}