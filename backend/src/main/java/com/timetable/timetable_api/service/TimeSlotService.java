package com.timetable.timetable_api.service;

import com.timetable.timetable_api.dto.TimeSlotRequest;
import com.timetable.timetable_api.model.TimeSlot;
import com.timetable.timetable_api.repository.TimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TimeSlotService {

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    public TimeSlot createTimeSlot(TimeSlotRequest request) {
        validate(request);

        TimeSlot timeSlot = new TimeSlot();
        timeSlot.setDayOfWeek(request.getDayOfWeek());
        timeSlot.setStartTime(request.getStartTime());
        timeSlot.setEndTime(request.getEndTime());
        timeSlot.setBreakSlot(request.isBreak());

        return timeSlotRepository.save(timeSlot);
    }

    public List<TimeSlot> getAllTimeSlots() {
        return timeSlotRepository.findAllByOrderByDayOfWeekAscStartTimeAsc();
    }

    public TimeSlot updateTimeSlot(Long id, TimeSlotRequest request) {
        validate(request);
        TimeSlot existing = timeSlotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Time slot not found with ID: " + id));

        existing.setDayOfWeek(request.getDayOfWeek());
        existing.setStartTime(request.getStartTime());
        existing.setEndTime(request.getEndTime());
        existing.setBreakSlot(request.isBreak());

        return timeSlotRepository.save(existing);
    }

    public void deleteTimeSlot(Long id) {
        if (id == null) {
            throw new RuntimeException("Time slot ID is required");
        }
        TimeSlot existing = timeSlotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Time slot not found with ID: " + id));
        timeSlotRepository.delete(existing);
    }

    private void validate(TimeSlotRequest request) {
        if (request.getDayOfWeek() == null || request.getDayOfWeek().isBlank()) {
            throw new RuntimeException("dayOfWeek is required");
        }
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new RuntimeException("startTime and endTime are required");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new RuntimeException("endTime must be after startTime");
        }
    }
}

