package com.timetable.timetable_api.controller;

import com.timetable.timetable_api.dto.FacultyTimetableEntry;
import com.timetable.timetable_api.model.ScheduledClass;
import com.timetable.timetable_api.repository.ScheduledClassRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/timetable/faculty")
public class FacultyTimetableController {

    @Autowired
    private ScheduledClassRepository scheduledClassRepository;

    @GetMapping("/{facultyId}")
    public ResponseEntity<List<FacultyTimetableEntry>> getFacultyTimetable(@PathVariable Long facultyId) {
        List<ScheduledClass> classes = scheduledClassRepository.findByCourseOffering_Faculty_Id(facultyId);

        List<FacultyTimetableEntry> timetable = classes.stream().map(sc -> new FacultyTimetableEntry(
                sc.getCourseOffering().getCourse().getCourseName(),
                sc.getCourseOffering().getCourse().getCourseCode(),
                sc.getDayOfWeek(),
                sc.getStartTime(),
                sc.getEndTime(),
                sc.getRoom().getRoomNumber(),
                sc.getCourseOffering().getSection().getName() + " (Sem "
                        + sc.getCourseOffering().getSection().getSemester() + ")"))
                .collect(Collectors.toList());

        return new ResponseEntity<>(timetable, HttpStatus.OK);
    }
}
