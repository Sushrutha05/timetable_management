package com.timetable.timetable_api.repository;

import com.timetable.timetable_api.model.TimetableMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TimetableMetadataRepository extends JpaRepository<TimetableMetadata, Long> {
    Optional<TimetableMetadata> findByKey(String key);
}
