package com.example.roombook.service;

import com.example.roombook.entity.Booking;
import com.example.roombook.entity.Employee;
import com.example.roombook.entity.Room;
import com.example.roombook.exception.BookingConflictException;
import com.example.roombook.exception.ResourceNotFoundException;
import com.example.roombook.repository.BookingRepository;
import com.example.roombook.repository.EmployeeRepository;
import com.example.roombook.repository.RoomRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final EmployeeRepository employeeRepository;

    public BookingService(
            BookingRepository bookingRepository,
            RoomRepository roomRepository,
            EmployeeRepository employeeRepository) {

        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.employeeRepository = employeeRepository;
    }

    // =========================
    // CREATE BOOKING
    // =========================
    public Booking createBooking(Booking booking) {

        validateTime(
                booking.getStartTime(),
                booking.getEndTime()
        );

        Room room = roomRepository.findById(
                        booking.getRoom().getId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Room not found"
                        )
                );

        Employee employee = employeeRepository.findById(
                        booking.getEmployee().getId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found"
                        )
                );

        checkConflict(
                room.getId(),
                booking.getStartTime(),
                booking.getEndTime(),
                null
        );

        booking.setRoom(room);
        booking.setEmployee(employee);

        booking.setStatus("CONFIRMED");
        booking.setCheckedIn(false);

        return bookingRepository.save(booking);
    }

    // =========================
    // GET ALL BOOKINGS
    // =========================
    public List<Booking> getAllBookings() {

        return bookingRepository.findAll();
    }

    // =========================
    // GET BOOKING BY ID
    // =========================
    public Booking getBookingById(Long id) {

        return bookingRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Booking not found with id: " + id
                        )
                );
    }

    // =========================
    // UPDATE BOOKING
    // =========================
    public Booking updateBooking(
            Long id,
            Booking booking) {

        Booking existingBooking =
                getBookingById(id);

        if ("CANCELLED".equals(
                existingBooking.getStatus())) {

            throw new IllegalArgumentException(
                    "Cancelled booking cannot be updated"
            );
        }

        if ("NO_SHOW".equals(
                existingBooking.getStatus())) {

            throw new IllegalArgumentException(
                    "No-show booking cannot be updated"
            );
        }

        validateTime(
                booking.getStartTime(),
                booking.getEndTime()
        );

        Room room = roomRepository.findById(
                        booking.getRoom().getId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Room not found"
                        )
                );

        Employee employee =
                employeeRepository.findById(
                                booking.getEmployee().getId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Employee not found"
                                )
                        );

        checkConflict(
                room.getId(),
                booking.getStartTime(),
                booking.getEndTime(),
                id
        );

        existingBooking.setRoom(room);
        existingBooking.setEmployee(employee);

        existingBooking.setStartTime(
                booking.getStartTime()
        );

        existingBooking.setEndTime(
                booking.getEndTime()
        );

        return bookingRepository.save(
                existingBooking
        );
    }

    // =========================
    // DELETE BOOKING
    // =========================
    public void deleteBooking(Long id) {

        Booking booking =
                getBookingById(id);

        bookingRepository.delete(booking);
    }

    // =========================
    // CANCEL BOOKING
    // =========================
    public Booking cancelBooking(Long id) {

        Booking booking =
                getBookingById(id);

        if ("CANCELLED".equals(
                booking.getStatus())) {

            throw new IllegalArgumentException(
                    "Booking is already cancelled"
            );
        }

        if ("NO_SHOW".equals(
                booking.getStatus())) {

            throw new IllegalArgumentException(
                    "No-show booking cannot be cancelled"
            );
        }

        booking.setStatus("CANCELLED");

        return bookingRepository.save(booking);
    }

    // =========================
    // CHECK IN
    // =========================
    public Booking checkIn(Long id) {

        Booking booking =
                getBookingById(id);

        if (!"CONFIRMED".equals(
                booking.getStatus())) {

            throw new IllegalArgumentException(
                    "Only confirmed bookings can be checked in"
            );
        }

        LocalDateTime graceTime =
                booking.getStartTime()
                        .plusMinutes(10);

        if (LocalDateTime.now()
                .isAfter(graceTime)) {

            booking.setStatus("NO_SHOW");

            bookingRepository.save(booking);

            throw new IllegalArgumentException(
                    "Check-in time expired. Booking released as no-show"
            );
        }

        booking.setCheckedIn(true);
        booking.setStatus("CHECKED_IN");

        return bookingRepository.save(booking);
    }

    // =========================
    // PAGINATION + SORTING
    // ADDITIONAL FEATURE
    // =========================
    public Page<Booking>
    getBookingsWithPaginationAndSorting(
            int page,
            int size,
            String sortBy) {

        PageRequest pageRequest =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(sortBy)
                );

        return bookingRepository.findAll(
                pageRequest
        );
    }

    // =========================
    // VALIDATE TIME
    // =========================
    private void validateTime(
            LocalDateTime startTime,
            LocalDateTime endTime) {

        if (startTime == null ||
                endTime == null) {

            throw new IllegalArgumentException(
                    "Start time and end time are required"
            );
        }

        if (!endTime.isAfter(startTime)) {

            throw new IllegalArgumentException(
                    "End time must be after start time"
            );
        }
    }

    // =========================
    // CHECK BOOKING CONFLICT
    // =========================
    private void checkConflict(
            Long roomId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long currentBookingId) {

        List<Booking> confirmedBookings =
                bookingRepository
                        .findByRoomIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                                roomId,
                                "CONFIRMED",
                                endTime,
                                startTime
                        );

        List<Booking> checkedInBookings =
                bookingRepository
                        .findByRoomIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                                roomId,
                                "CHECKED_IN",
                                endTime,
                                startTime
                        );

        boolean confirmedConflict =
                confirmedBookings.stream()
                        .anyMatch(booking ->
                                currentBookingId == null ||
                                        !booking.getId()
                                                .equals(currentBookingId)
                        );

        boolean checkedInConflict =
                checkedInBookings.stream()
                        .anyMatch(booking ->
                                currentBookingId == null ||
                                        !booking.getId()
                                                .equals(currentBookingId)
                        );

        if (confirmedConflict ||
                checkedInConflict) {

            throw new BookingConflictException(
                    "Room is already booked for the selected time slot"
            );
        }
    }

    // =========================
    // AUTO RELEASE NO-SHOW
    // Runs every 1 minute
    // =========================
    @Scheduled(fixedRate = 60000)
    public void releaseNoShowBookings() {

        LocalDateTime graceTime =
                LocalDateTime.now()
                        .minusMinutes(10);

        List<Booking> bookings =
                bookingRepository
                        .findByStatusAndCheckedInFalseAndStartTimeBefore(
                                "CONFIRMED",
                                graceTime
                        );

        for (Booking booking : bookings) {

            booking.setStatus("NO_SHOW");

            bookingRepository.save(booking);
        }
    }
}