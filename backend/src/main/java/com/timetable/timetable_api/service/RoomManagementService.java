package com.timetable.timetable_api.service;

import com.timetable.timetable_api.dto.RoomCreationRequest;
import com.timetable.timetable_api.model.Room;
import com.timetable.timetable_api.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
     * Bulk create rooms from CSV input.
     */
    public List<Room> bulkCreateRooms(InputStream inputStream) {
        List<Room> createdRooms = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new RuntimeException("CSV file is empty.");
            }

            Map<String, Integer> headerIndex = mapHeaderIndexes(headerLine);
            validateRequiredHeaders(headerIndex);

            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] values = splitCsvLine(line);
                RoomCreationRequest request = buildRequestFromRow(values, headerIndex, rowNumber);
                createdRooms.add(createRoom(request));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSV file: " + e.getMessage(), e);
        }

        return createdRooms;
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

    private Map<String, Integer> mapHeaderIndexes(String headerLine) {
        String[] headers = splitCsvLine(headerLine);
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            index.put(headers[i].trim().toLowerCase(Locale.ROOT), i);
        }
        return index;
    }

    private void validateRequiredHeaders(Map<String, Integer> headerIndex) {
        List<String> requiredHeaders = List.of("roomnumber", "type", "capacity");
        for (String header : requiredHeaders) {
            if (!headerIndex.containsKey(header)) {
                throw new RuntimeException("Missing required CSV header: " + header);
            }
        }
    }

    private RoomCreationRequest buildRequestFromRow(String[] values, Map<String, Integer> headerIndex, int rowNumber) {
        try {
            RoomCreationRequest request = new RoomCreationRequest();
            request.setRoomNumber(getValue(values, headerIndex, "roomnumber"));
            request.setType(getValue(values, headerIndex, "type"));
            request.setCapacity(Integer.parseInt(getValue(values, headerIndex, "capacity")));
            return request;
        } catch (NumberFormatException ex) {
            throw new RuntimeException("Invalid number format on row " + rowNumber + ": " + ex.getMessage(), ex);
        }
    }

    private String getValue(String[] values, Map<String, Integer> headerIndex, String key) {
        Integer idx = headerIndex.get(key);
        if (idx == null || idx >= values.length) {
            return "";
        }
        String raw = values[idx];
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String[] splitCsvLine(String line) {
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    }
}
