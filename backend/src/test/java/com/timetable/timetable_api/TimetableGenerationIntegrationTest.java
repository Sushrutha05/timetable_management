package com.timetable.timetable_api;

import com.timetable.timetable_api.model.*;
import com.timetable.timetable_api.repository.*;
import com.timetable.timetable_api.service.TimetableGenerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TimetableGenerationIntegrationTest {

    @Autowired
    private TimetableGenerationService generationService;

    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private FacultyRepository facultyRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private SectionRepository sectionRepository;
    @Autowired
    private TimeSlotRepository timeSlotRepository;
    @Autowired
    private CourseOfferingRepository offeringRepository;
    @Autowired
    private ScheduledClassRepository scheduledClassRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private DesignationConstraintRepository designationConstraintRepository;

    @Test
    public void testLtpScheduling() {
        // 1. Setup Data
        Department dept = new Department();
        dept.setName("CSE");
        // dept.setCode("CSE"); // Department has no code
        departmentRepository.save(dept);

        User user = new User();
        user.setEmail("smith@test.com");
        user.setPasswordHash("hash");
        user.setRole(2);
        userRepository.save(user);

        DesignationConstraint dc = new DesignationConstraint();
        dc.setDesignation("Professor");
        dc.setMaxLectureHours(10);
        dc.setMaxLabHours(10);
        dc.setMaxTotalHours(20);
        dc.setPriorityLevel(1);
        designationConstraintRepository.save(dc);

        Faculty faculty = new Faculty();
        faculty.setFirstName("Dr.");
        faculty.setLastName("Smith");
        faculty.setUser(user);
        faculty.setDepartment(dept);
        faculty.setDesignationConstraint(dc);
        facultyRepository.save(faculty);

        Course course = new Course();
        course.setCourseCode("CS101");
        course.setCourseName("Intro to CS");
        course.setCreditHours(4);
        course.setLectureHours(2); // 2 hours Lecture
        course.setTutorialHours(1); // 1 hour Tutorial
        course.setPracticalHours(3); // 3 hours Practical (1 block)
        // course.setDepartment(dept); // Course has no department
        courseRepository.save(course);

        Section section = new Section();
        section.setName("A");
        section.setSemester(1);
        section.setYear(2025);
        section.setDepartment(dept);
        section.setStudentCount(30);
        sectionRepository.save(section);

        CourseOffering offering = new CourseOffering();
        offering.setCourse(course);
        offering.setFaculty(faculty);
        offering.setSection(section);
        offeringRepository.save(offering);

        Room labRoom = new Room();
        labRoom.setRoomNumber("LAB-1");
        labRoom.setType("LAB");
        labRoom.setCapacity(40);
        roomRepository.save(labRoom);

        Room classRoom = new Room();
        classRoom.setRoomNumber("CR-1");
        classRoom.setType("CLASSROOM");
        classRoom.setCapacity(40);
        roomRepository.save(classRoom);

        // Create standard slots: Mon 9-10, 10-11, 11-12, 12-1(Break), 1-2, 2-3, 3-4,
        // 4-5
        createSlot("MONDAY", "09:00", "10:00", false);
        createSlot("MONDAY", "10:00", "11:00", false);
        createSlot("MONDAY", "11:00", "12:00", false);
        createSlot("MONDAY", "12:00", "13:00", true); // Break
        createSlot("MONDAY", "13:00", "14:00", false);
        createSlot("MONDAY", "14:00", "15:00", false);
        createSlot("MONDAY", "15:00", "16:00", false);

        // Tue for overflow
        createSlot("TUESDAY", "09:00", "10:00", false);
        createSlot("TUESDAY", "10:00", "11:00", false);

        // 2. Generate
        List<ScheduledClass> scheduled = generationService.generateTimetable();

        // 3. Verify
        assertFalse(scheduled.isEmpty());

        // Check Practical: Should have 3 consecutive slots on the SAME DAY
        long practicals = scheduled.stream()
                .filter(sc -> sc.getRoom().getType().equals("LAB"))
                .count();
        assertEquals(3, practicals, "Should have 3 practical slots assigned");

        // Check Theory (L+T): Should have 2+1 = 3 slots
        long theory = scheduled.stream()
                .filter(sc -> !sc.getRoom().getType().equals("LAB"))
                .count();
        assertEquals(3, theory, "Should have 3 theory slots (2L + 1T)");

        // Check continuity of Practical
        // ... (can implement detailed check)
    }

    private void createSlot(String day, String start, String end, boolean isBreak) {
        TimeSlot ts = new TimeSlot();
        ts.setDayOfWeek(day);
        ts.setStartTime(LocalTime.parse(start));
        ts.setEndTime(LocalTime.parse(end));
        ts.setBreakSlot(isBreak);
        ts.setSemesterGroup("SEM_1_2");
        timeSlotRepository.save(ts);
    }
}
