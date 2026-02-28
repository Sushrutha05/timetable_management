package com.timetable.timetable_api.service;

import com.timetable.timetable_api.model.ScheduledClass;
import com.timetable.timetable_api.repository.ScheduledClassRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TimetableViewService {

    @Autowired
    private ScheduledClassRepository scheduledClassRepository;

    /**
     * Gets the entire generated timetable. (For Admin)
     */
    public List<ScheduledClass> getFullTimetable() {
        return scheduledClassRepository.findAll();
    }

    /**
     * Gets only the classes for a specific faculty member. (For Faculty)
     */
    public List<ScheduledClass> getTimetableForFaculty(Long facultyId) {
        // This requires a custom method in our repository
        return scheduledClassRepository.findByCourseOffering_Faculty_Id(facultyId);
    }

    /**
     * Gets only the classes for a specific section. (For Admin Section View)
     */
    public List<ScheduledClass> getTimetableForSection(Long sectionId) {
        return scheduledClassRepository.findByCourseOffering_Section_Id(sectionId);
    }
}