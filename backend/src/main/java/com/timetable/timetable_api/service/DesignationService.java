package com.timetable.timetable_api.service;

import com.timetable.timetable_api.dto.DesignationRequest;
import com.timetable.timetable_api.model.DesignationConstraint;
import com.timetable.timetable_api.repository.DesignationConstraintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DesignationService {

    @Autowired
    private DesignationConstraintRepository designationRepository;

    /**
     * Creates or Updates a Designation Constraint.
     * The 'designation' string is the Primary Key, so saving
     * with an existing key will automatically perform an update.
     */
    public DesignationConstraint createOrUpdateDesignation(DesignationRequest request) {
        DesignationConstraint constraint = new DesignationConstraint();
        constraint.setDesignation(request.getDesignation());
        constraint.setMaxLectureHours(request.getMaxLectureHours());
        constraint.setMaxLabHours(request.getMaxLabHours());

        // This is an "upsert": creates if new, updates if exists.
        return designationRepository.save(constraint);
    }

    /**
     * Gets a list of all designation constraints.
     */
    public List<DesignationConstraint> getAllDesignations() {
        return designationRepository.findAll();
    }

    /**
     * Gets a single designation by its ID (the string).
     */
    public DesignationConstraint getDesignationById(String designation) {
        return designationRepository.findById(designation).orElse(null);
    }

    /**
     * Deletes a designation constraint.
     */
    public void deleteDesignation(String designation) {
        designationRepository.deleteById(designation);
    }
}