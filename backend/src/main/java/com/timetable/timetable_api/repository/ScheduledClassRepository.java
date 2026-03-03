package com.timetable.timetable_api.repository;

import com.timetable.timetable_api.model.ScheduledClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ScheduledClassRepository extends JpaRepository<ScheduledClass, Long> {
    // Primary Key 'class_id' is a Long

    List<ScheduledClass> findByCourseOffering_Faculty_Id(Long facultyId); // Retained for compatibility if needed

    @Query("SELECT s FROM ScheduledClass s WHERE s.assignedFaculty.id = :facultyId OR (s.assignedFaculty IS NULL AND s.courseOffering.faculty.id = :facultyId)")
    List<ScheduledClass> findForFaculty(@Param("facultyId") Long facultyId);

    List<ScheduledClass> findByCourseOffering_Section_Id(Long sectionId);

    List<ScheduledClass> findByDayOfWeekAndStartTime(String dayOfWeek, java.time.LocalTime startTime);

    List<ScheduledClass> findByRoom_IdAndDayOfWeekAndStartTime(Integer roomId, String dayOfWeek,
            java.time.LocalTime startTime);

    // Used by cascade delete: remove scheduled classes for a course offering
    void deleteByCourseOfferingId(Long courseOfferingId);

    // Used by cascade delete: remove all scheduled classes for a given course (via
    // offering)
    void deleteByCourseOffering_CourseId(Long courseId);
}