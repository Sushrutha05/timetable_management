package com.timetable.timetable_api.repository;

import com.timetable.timetable_api.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {
    // Primary Key 'section_id' is a Long
}