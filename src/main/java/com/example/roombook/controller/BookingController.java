package com.example.roombook.controller;

import com.example.roombook.entity.Booking;
import com.example.roombook.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
@CrossOrigin
public class BookingController {

    private final BookingService bookingService;

    public BookingController(
            BookingService bookingService) {

        this.bookingService =
                bookingService;
    }


    @PostMapping
    public ResponseEntity<Booking> createBooking(
            @Valid @RequestBody Booking booking) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        bookingService
                                .createBooking(booking)
                );
    }


    @GetMapping
    public List<Booking> getAllBookings() {

        return bookingService
                .getAllBookings();
    }


    @GetMapping("/{id}")
    public Booking getBooking(
            @PathVariable Long id) {

        return bookingService
                .getBookingById(id);
    }


    @PutMapping("/{id}")
    public Booking updateBooking(
            @PathVariable Long id,
            @Valid @RequestBody Booking booking) {

        return bookingService
                .updateBooking(id, booking);
    }


    @PutMapping("/{id}/cancel")
    public Booking cancelBooking(
            @PathVariable Long id) {

        return bookingService
                .cancelBooking(id);
    }


    @PutMapping("/{id}/checkin")
    public Booking checkIn(
            @PathVariable Long id) {

        return bookingService
                .checkIn(id);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBooking(
            @PathVariable Long id) {

        bookingService.deleteBooking(id);

        return ResponseEntity.ok(
                "Booking deleted successfully"
        );
    }
}