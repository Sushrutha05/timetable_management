package com.timetable.timetable_api.service;

import com.timetable.timetable_api.dto.DepartmentCreationRequest;
import com.timetable.timetable_api.model.Department;
import com.timetable.timetable_api.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DepartmentCourseRepository departmentCourseRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private SectionRepository sectionRepository;

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public Department createDepartment(DepartmentCreationRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("Department name cannot be empty");
        }
        if (departmentRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new RuntimeException("Department with this name already exists");
        }
        Department department = new Department();
        department.setName(request.getName().trim());
        return departmentRepository.save(department);
    }

    public Department updateDepartment(Integer id, DepartmentCreationRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("Department name cannot be empty");
        }

        String newName = request.getName().trim();
        if (!department.getName().equalsIgnoreCase(newName) && departmentRepository.existsByNameIgnoreCase(newName)) {
            throw new RuntimeException("Department with this name already exists");
        }

        department.setName(newName);
        return departmentRepository.save(department);
    }

    public void deleteDepartment(Integer id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        if (departmentCourseRepository.countByDepartmentId(id) > 0) {
            throw new RuntimeException("Cannot delete department because it is associated with courses");
        }

        if (facultyRepository.countByDepartmentId(id) > 0) {
            throw new RuntimeException("Cannot delete department because it is associated with faculty members");
        }

        if (sectionRepository.countByDepartmentId(id) > 0) {
            throw new RuntimeException("Cannot delete department because it is associated with sections");
        }

        departmentRepository.delete(department);
    }
}
