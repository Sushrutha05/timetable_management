package com.timetable.timetable_api.dto;

public class RoomCreationRequest {

    private String roomNumber;
    private String type; // e.g., "CLASSROOM" or "LAB"
    private Integer capacity;

    // --- Getters and Setters ---

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }
}