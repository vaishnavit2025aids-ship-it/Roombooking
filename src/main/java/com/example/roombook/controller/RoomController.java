package com.example.roombook.controller;

import com.example.roombook.entity.Room;
import com.example.roombook.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rooms")
@CrossOrigin
public class RoomController {

    private final RoomService roomService;

    public RoomController(
            RoomService roomService) {

        this.roomService = roomService;
    }


    @PostMapping
    public ResponseEntity<Room> createRoom(
            @Valid @RequestBody Room room) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        roomService.createRoom(room)
                );
    }


    @GetMapping
    public List<Room> getAllRooms() {

        return roomService.getAllRooms();
    }


    @GetMapping("/{id}")
    public Room getRoomById(
            @PathVariable Long id) {

        return roomService.getRoomById(id);
    }


    @PutMapping("/{id}")
    public Room updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody Room room) {

        return roomService.updateRoom(
                id,
                room
        );
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRoom(
            @PathVariable Long id) {

        roomService.deleteRoom(id);

        return ResponseEntity.ok(
                "Room deleted successfully"
        );
    }
}