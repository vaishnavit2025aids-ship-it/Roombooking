package com.example.roombook.service;

import com.example.roombook.entity.Room;
import com.example.roombook.exception.ResourceNotFoundException;
import com.example.roombook.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }


    public Room createRoom(Room room) {

        return roomRepository.save(room);
    }


    public List<Room> getAllRooms() {

        return roomRepository.findAll();
    }


    public Room getRoomById(Long id) {

        return roomRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Room not found with id: " + id
                        )
                );
    }


    public Room updateRoom(Long id, Room room) {

        Room existingRoom = getRoomById(id);

        existingRoom.setRoomName(
                room.getRoomName()
        );

        existingRoom.setCapacity(
                room.getCapacity()
        );

        existingRoom.setProjector(room.getProjector());
        existingRoom.setWhiteboard(room.getWhiteboard());

        return roomRepository.save(existingRoom);
    }


    public void deleteRoom(Long id) {

        Room room = getRoomById(id);

        roomRepository.delete(room);
    }
}