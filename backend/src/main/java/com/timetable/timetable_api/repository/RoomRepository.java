package com.timetable.timetable_api.repository;

import com.timetable.timetable_api.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomRepository extends JpaRepository<Room, Integer> {
    // Primary Key 'room_id' is an Integer
}