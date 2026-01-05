package com.timetable.timetable_api.dto;

import java.time.LocalTime;

public class UpdateSlotRequest {
    private Long scheduledClassId;
    private Integer newRoomId;
    private String newDayOfWeek;
    private LocalTime newStartTime;

    public Long getScheduledClassId() {
        return scheduledClassId;
    }

    public void setScheduledClassId(Long scheduledClassId) {
        this.scheduledClassId = scheduledClassId;
    }

    public Integer getNewRoomId() {
        return newRoomId;
    }

    public void setNewRoomId(Integer newRoomId) {
        this.newRoomId = newRoomId;
    }

    public String getNewDayOfWeek() {
        return newDayOfWeek;
    }

    public void setNewDayOfWeek(String newDayOfWeek) {
        this.newDayOfWeek = newDayOfWeek;
    }

    public LocalTime getNewStartTime() {
        return newStartTime;
    }

    public void setNewStartTime(LocalTime newStartTime) {
        this.newStartTime = newStartTime;
    }
}
