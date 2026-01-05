package com.timetable.timetable_api.service;

import com.timetable.timetable_api.model.*;
import com.timetable.timetable_api.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TimetableAssignmentService {

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private FacultyPreferenceRepository facultyPreferenceRepository;

    @Autowired
    private DesignationConstraintRepository designationConstraintRepository;

    @Autowired
    private CourseOfferingRepository courseOfferingRepository;

    @Autowired
    private DepartmentCourseRepository departmentCourseRepository;

    public Map<String, Object> getAssignmentData(Long sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Section not found with ID: " + sectionId));
        Department department = section.getDepartment();

        // 1) Faculty Pool with workload and max hours
        List<Faculty> faculties = facultyRepository.findAll();
        List<Map<String, Object>> facultyPool = faculties.stream().map(f -> {
            Long assigned = Optional.ofNullable(courseOfferingRepository.getTotalCreditHoursByFaculty(f.getId())).orElse(0L);
            DesignationConstraint dc = f.getDesignationConstraint();
            Integer maxHours = null;
            if (dc != null) {
                Integer lec = Optional.ofNullable(dc.getMaxLectureHours()).orElse(0);
                Integer lab = Optional.ofNullable(dc.getMaxLabHours()).orElse(0);
                maxHours = lec + lab; // Total allowed teaching hours
            }
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", f.getId());
            map.put("firstName", f.getFirstName());
            map.put("lastName", f.getLastName());
            map.put("departmentId", f.getDepartment() != null ? f.getDepartment().getId() : null);
            map.put("designation", dc != null ? dc.getDesignation() : null);
            map.put("maxAllowedHours", maxHours);
            map.put("currentAssignedHours", assigned);
            return map;
        }).collect(Collectors.toList());

        // 2) Course Needs for the Section's Department with preference hints (accurate via DepartmentCourse)
        List<DepartmentCourse> dcLinks = departmentCourseRepository.findByDepartmentId(department.getId());
        List<Course> deptCourses = dcLinks.stream()
                .map(DepartmentCourse::getCourse)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // preferences by course id
        List<FacultyPreference> prefs = facultyPreferenceRepository.findAll();
        Map<Long, List<Map<String, Object>>> prefByCourse = new HashMap<>();
        for (FacultyPreference p : prefs) {
            if (p.getCourse() == null) continue;
            Long courseId = p.getCourse().getId();
            prefByCourse.computeIfAbsent(courseId, k -> new ArrayList<>()).add(Map.of(
                    "facultyId", p.getFaculty() != null ? p.getFaculty().getId() : null,
                    "priority", p.getPriority()
            ));
        }

        List<Map<String, Object>> courseNeeds = deptCourses.stream().map(c -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", c.getId());
            map.put("courseCode", c.getCourseCode());
            map.put("courseName", c.getCourseName());
            map.put("creditHours", c.getCreditHours());
            map.put("preferences", prefByCourse.getOrDefault(c.getId(), List.of()));
            return map;
        }).collect(Collectors.toList());

        // 3) Existing Offerings for this section
        List<CourseOffering> offerings = courseOfferingRepository.findBySectionId(sectionId);
        List<Map<String, Object>> existingOfferings = offerings.stream().map(o -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", o.getId());
            map.put("courseId", o.getCourse() != null ? o.getCourse().getId() : null);
            map.put("courseName", o.getCourse() != null ? o.getCourse().getCourseName() : null);
            map.put("courseCode", o.getCourse() != null ? o.getCourse().getCourseCode() : null);
            map.put("facultyId", o.getFaculty() != null ? o.getFaculty().getId() : null);
            map.put("facultyName", o.getFaculty() != null ? (o.getFaculty().getFirstName()+" "+o.getFaculty().getLastName()) : null);
            map.put("sectionId", sectionId);
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("section", Map.of(
                "id", section.getId(),
                "name", section.getName(),
                "departmentId", department.getId()
        ));
        response.put("facultyPool", facultyPool);
        response.put("courseNeeds", courseNeeds);
        response.put("existingOfferings", existingOfferings);
        return response;
    }
}
