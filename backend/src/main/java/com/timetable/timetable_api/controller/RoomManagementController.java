package com.timetable.timetable_api.controller;

import com.timetable.timetable_api.dto.RoomCreationRequest;
import com.timetable.timetable_api.model.Room;
import com.timetable.timetable_api.service.RoomManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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
     * Update a Room.
     * Endpoint: PUT /api/admin/room/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRoom(@PathVariable Integer id, @RequestBody RoomCreationRequest request) {
        try {
            Room updatedRoom = roomService.updateRoom(id, request);
            return new ResponseEntity<>(updatedRoom, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Delete a Room.
     * Endpoint: DELETE /api/admin/room/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRoom(@PathVariable Integer id) {
        try {
            roomService.deleteRoom(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Bulk upload rooms via CSV.
     * Endpoint: POST /api/admin/room/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<?> bulkUploadRooms(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Uploaded file is empty."));
        }

        try {
            List<Room> createdRooms = roomService.bulkCreateRooms(file.getInputStream());
            return new ResponseEntity<>(createdRooms, HttpStatus.CREATED);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to read uploaded file: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Bulk upload failed: " + e.getMessage()));
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
