package com.timetable.timetable_api.controller;

import com.timetable.timetable_api.dto.TimeSlotRequest;
import com.timetable.timetable_api.model.TimeSlot;
import com.timetable.timetable_api.service.TimeSlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/timeslot")
public class TimeSlotController {

    @Autowired
    private TimeSlotService timeSlotService;

    @PostMapping
    public ResponseEntity<?> createTimeSlot(@RequestBody TimeSlotRequest request) {
        try {
            TimeSlot created = timeSlotService.createTimeSlot(request);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    public ResponseEntity<List<TimeSlot>> getAllTimeSlots() {
        List<TimeSlot> slots = timeSlotService.getAllTimeSlots();
        return new ResponseEntity<>(slots, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTimeSlot(@PathVariable Long id, @RequestBody TimeSlotRequest request) {
        try {
            TimeSlot updated = timeSlotService.updateTimeSlot(id, request);
            return new ResponseEntity<>(updated, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTimeSlot(@PathVariable Long id) {
        try {
            timeSlotService.deleteTimeSlot(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}
