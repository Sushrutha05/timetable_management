package com.timetable.timetable_api.controller;

import com.timetable.timetable_api.dto.RoomCreationRequest;
import com.timetable.timetable_api.model.Room;
import com.timetable.timetable_api.service.RoomManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/room") // Base URL for this controller
public class RoomManagementController {

    @Autowired
    private RoomManagementService roomService;

    /**
     * Create a new Room.
     * Endpoint: POST /api/admin/room
     */
    @PostMapping
    public ResponseEntity<?> createRoom(@RequestBody RoomCreationRequest request) {
        try {
            Room savedRoom = roomService.createRoom(request);
            return new ResponseEntity<>(savedRoom, HttpStatus.CREATED);
        } catch (Exception e) {
            // Catches errors like duplicate room_number
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Get all Rooms.
     * Endpoint: GET /api/admin/room
     */
    @GetMapping
    public ResponseEntity<List<Room>> getAllRooms() {
        List<Room> rooms = roomService.getAllRooms();
        return new ResponseEntity<>(rooms, HttpStatus.OK);
    }

    /**
     * Get a single Room by ID.
     * Endpoint: GET /api/admin/room/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Room> getRoomById(@PathVariable Integer id) {
        Room room = roomService.getRoomById(id);
        if (room != null) {
            return new ResponseEntity<>(room, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}