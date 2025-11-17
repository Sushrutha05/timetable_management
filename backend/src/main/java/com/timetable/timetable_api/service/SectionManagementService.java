package com.timetable.timetable_api.service;

import com.timetable.timetable_api.dto.SectionCreationRequest;
import com.timetable.timetable_api.model.Department;
import com.timetable.timetable_api.model.Section;
import com.timetable.timetable_api.repository.DepartmentRepository;
import com.timetable.timetable_api.repository.SectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SectionManagementService {

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private DepartmentRepository departmentRepository; // We need this to link the department

    /**
     * Creates a new Section.
     */
    public Section createSection(SectionCreationRequest request) {
        // 1. Validate and fetch the Department
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Department not found with ID: " + request.getDepartmentId()));

        // 2. Create the new Section
        Section newSection = new Section();
        newSection.setDepartment(department); // Link the department object
        newSection.setName(request.getName());
        newSection.setSemester(request.getSemester());
        newSection.setYear(request.getYear());

        return sectionRepository.save(newSection);
    }

    /**
     * Gets a list of all sections.
     */
    public List<Section> getAllSections() {
        return sectionRepository.findAll();
    }

    /**
     * Gets a single section by its ID.
     */
    public Section getSectionById(Long id) {
        return sectionRepository.findById(id).orElse(null);
    }
}