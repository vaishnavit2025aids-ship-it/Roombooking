package com.example.roombook.controller;

import com.example.roombook.entity.Booking;
import com.example.roombook.service.BookingService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
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

        this.bookingService = bookingService;
    }

    // =========================
    // CREATE
    // =========================
    @PostMapping
    public ResponseEntity<Booking> createBooking(
            @Valid @RequestBody Booking booking) {

        Booking createdBooking =
                bookingService.createBooking(booking);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdBooking);
    }

    // =========================
    // GET ALL
    // =========================
    @GetMapping
    public List<Booking> getAllBookings() {

        return bookingService.getAllBookings();
    }

    // =========================
    // PAGINATION + SORTING
    // ADDITIONAL FEATURE
    // =========================
    @GetMapping("/paged")
    public Page<Booking>
    getBookingsWithPaginationAndSorting(

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "5")
            int size,

            @RequestParam(defaultValue = "startTime")
            String sortBy) {

        return bookingService
                .getBookingsWithPaginationAndSorting(
                        page,
                        size,
                        sortBy
                );
    }

    // =========================
    // GET BY ID
    // =========================
    @GetMapping("/{id}")
    public Booking getBookingById(
            @PathVariable Long id) {

        return bookingService
                .getBookingById(id);
    }

    // =========================
    // UPDATE
    // =========================
    @PutMapping("/{id}")
    public Booking updateBooking(
            @PathVariable Long id,
            @Valid @RequestBody Booking booking) {

        return bookingService
                .updateBooking(id, booking);
    }

    // =========================
    // CANCEL
    // =========================
    @PutMapping("/{id}/cancel")
    public Booking cancelBooking(
            @PathVariable Long id) {

        return bookingService
                .cancelBooking(id);
    }

    // =========================
    // CHECK IN
    // =========================
    @PutMapping("/{id}/checkin")
    public Booking checkIn(
            @PathVariable Long id) {

        return bookingService
                .checkIn(id);
    }

    // =========================
    // DELETE
    // =========================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(
            @PathVariable Long id) {

        bookingService.deleteBooking(id);

        return ResponseEntity
                .noContent()
                .build();
    }
}