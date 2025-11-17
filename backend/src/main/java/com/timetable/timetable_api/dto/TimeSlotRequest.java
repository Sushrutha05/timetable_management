package com.timetable.timetable_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalTime;

public class TimeSlotRequest {

    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;

    @JsonProperty("isBreak")
    private boolean breakSlot;

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    @JsonProperty("isBreak")
    public boolean isBreak() {
        return breakSlot;
    }

    public void setBreak(boolean breakSlot) {
        this.breakSlot = breakSlot;
    }
}

