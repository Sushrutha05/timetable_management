package com.timetable.timetable_api.service;

import com.timetable.timetable_api.dto.RoomCreationRequest;
import com.timetable.timetable_api.model.Room;
import com.timetable.timetable_api.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoomManagementService {

    @Autowired
    private RoomRepository roomRepository;

    /**
     * Creates a new Room.
     */
    public Room createRoom(RoomCreationRequest request) {
        Room newRoom = new Room();
        newRoom.setRoomNumber(request.getRoomNumber());
        newRoom.setType(request.getType());
        newRoom.setCapacity(request.getCapacity());

        return roomRepository.save(newRoom);
    }

    /**
     * Gets a list of all rooms.
     */
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    /**
     * Gets a single room by its ID.
     */
    public Room getRoomById(Integer id) {
        return roomRepository.findById(id).orElse(null);
    }
}