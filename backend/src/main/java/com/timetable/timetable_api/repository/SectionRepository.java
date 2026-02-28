package com.timetable.timetable_api.repository;

import com.timetable.timetable_api.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findByDepartmentId(Integer departmentId);

    long countByDepartmentId(Integer departmentId);
}