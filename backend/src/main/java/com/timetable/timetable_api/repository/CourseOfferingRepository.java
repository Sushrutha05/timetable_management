package com.timetable.timetable_api.repository;

import com.timetable.timetable_api.model.CourseOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {

    // Find all offerings for a specific section
    List<CourseOffering> findBySectionId(Long sectionId);

    // Find all offerings for a specific faculty
    List<CourseOffering> findByFacultyId(Long facultyId);

    // Find all offerings for a specific course
    List<CourseOffering> findByCourseId(Long courseId);

    // Find offerings by section and course
    List<CourseOffering> findBySectionIdAndCourseId(Long sectionId, Long courseId);

    // Find offerings by faculty and section
    List<CourseOffering> findByFacultyIdAndSectionId(Long facultyId, Long sectionId);

    // Custom query to get total credit hours assigned to a faculty
    @Query("SELECT COALESCE(SUM(c.creditHours), 0) FROM CourseOffering co JOIN co.course c WHERE co.faculty.id = :facultyId")
    Long getTotalCreditHoursByFaculty(@Param("facultyId") Long facultyId);

    // Custom query to get total credit hours assigned to a faculty for a specific section
    @Query("SELECT COALESCE(SUM(c.creditHours), 0) FROM CourseOffering co JOIN co.course c WHERE co.faculty.id = :facultyId AND co.section.id = :sectionId")
    Long getTotalCreditHoursByFacultyAndSection(@Param("facultyId") Long facultyId, @Param("sectionId") Long sectionId);
}
