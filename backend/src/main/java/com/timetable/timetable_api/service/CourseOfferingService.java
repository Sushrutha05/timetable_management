package com.timetable.timetable_api.service;

import com.timetable.timetable_api.dto.CourseOfferingRequest;
import com.timetable.timetable_api.model.Course;
import com.timetable.timetable_api.model.CourseOffering;
import com.timetable.timetable_api.model.Faculty;
import com.timetable.timetable_api.model.Section;
import com.timetable.timetable_api.repository.CourseOfferingRepository;
import com.timetable.timetable_api.repository.CourseRepository;
import com.timetable.timetable_api.repository.FacultyRepository;
import com.timetable.timetable_api.repository.SectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CourseOfferingService {

    @Autowired
    private CourseOfferingRepository offeringRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private FacultyRepository facultyRepository;
    @Autowired
    private SectionRepository sectionRepository;

    /**
     * Creates a new CourseOffering (Admin's official assignment).
     */
    @Transactional
    public CourseOffering createOffering(CourseOfferingRequest request) {
        // 1. Validate and fetch all entities
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found with ID: " + request.getCourseId()));

        Faculty faculty = facultyRepository.findById(request.getFacultyId())
                .orElseThrow(() -> new RuntimeException("Faculty not found with ID: " + request.getFacultyId()));

        Section section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new RuntimeException("Section not found with ID: " + request.getSectionId()));

        // 2. Create the new offering
        CourseOffering newOffering = new CourseOffering();
        newOffering.setCourse(course);
        newOffering.setFaculty(faculty);
        newOffering.setSection(section);

        // You could add validation here, e.g., check if section already has this course
        // Or check if faculty is over-assigned (based on designation constraints)

        return offeringRepository.save(newOffering);
    }

    /**
     * Gets a list of all current offerings.
     */
    public List<CourseOffering> getAllOfferings() {
        return offeringRepository.findAll();
    }

    /**
     * Deletes an offering.
     */
    public void deleteOffering(Long offeringId) {
        // First check if it exists
        if (!offeringRepository.existsById(offeringId)) {
            throw new RuntimeException("Offering not found with ID: " + offeringId);
        }
        offeringRepository.deleteById(offeringId);
    }
}