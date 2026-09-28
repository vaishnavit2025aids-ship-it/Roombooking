package com.example.roombook.service;

import com.example.roombook.entity.Booking;
import com.example.roombook.entity.Employee;
import com.example.roombook.entity.Room;
import com.example.roombook.exception.BookingConflictException;
import com.example.roombook.exception.ResourceNotFoundException;
import com.example.roombook.repository.BookingRepository;
import com.example.roombook.repository.EmployeeRepository;
import com.example.roombook.repository.RoomRepository;
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

        Room room = getRoom(
                booking.getRoom().getId()
        );

        Employee employee = getEmployee(
                booking.getEmployee().getId()
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
    // READ ALL
    // =========================

    public List<Booking> getAllBookings() {

        return bookingRepository.findAll();
    }


    // =========================
    // READ BY ID
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
            Booking updatedBooking) {

        Booking existing =
                getBookingById(id);

        if ("CANCELLED".equals(existing.getStatus())
                || "NO_SHOW".equals(existing.getStatus())) {

            throw new IllegalArgumentException(
                    "Cancelled or no-show booking cannot be updated"
            );
        }

        validateTime(
                updatedBooking.getStartTime(),
                updatedBooking.getEndTime()
        );

        Room room = getRoom(
                updatedBooking.getRoom().getId()
        );

        Employee employee = getEmployee(
                updatedBooking.getEmployee().getId()
        );

        checkConflict(
                room.getId(),
                updatedBooking.getStartTime(),
                updatedBooking.getEndTime(),
                id
        );

        existing.setRoom(room);
        existing.setEmployee(employee);

        existing.setStartTime(
                updatedBooking.getStartTime()
        );

        existing.setEndTime(
                updatedBooking.getEndTime()
        );

        return bookingRepository.save(existing);
    }


    // =========================
    // DELETE
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
    // CHECK-IN
    // =========================

    public Booking checkIn(Long id) {

        Booking booking =
                getBookingById(id);

        if (!"CONFIRMED".equals(
                booking.getStatus())) {

            throw new IllegalArgumentException(
                    "Only confirmed booking can be checked in"
            );
        }

        LocalDateTime now =
                LocalDateTime.now();

        LocalDateTime allowedUntil =
                booking.getStartTime()
                        .plusMinutes(10);

        if (now.isAfter(allowedUntil)) {

            booking.setStatus("NO_SHOW");

            bookingRepository.save(booking);

            throw new IllegalArgumentException(
                    "Check-in time expired. Booking released."
            );
        }

        booking.setCheckedIn(true);
        booking.setStatus("CHECKED_IN");

        return bookingRepository.save(booking);
    }


    // =========================
    // TIME VALIDATION
    // =========================

    private void validateTime(
            LocalDateTime start,
            LocalDateTime end) {

        if (start == null || end == null) {

            throw new IllegalArgumentException(
                    "Start time and end time are required"
            );
        }

        if (!end.isAfter(start)) {

            throw new IllegalArgumentException(
                    "End time must be after start time"
            );
        }
    }


    // =========================
    // GET ROOM
    // =========================

    private Room getRoom(Long roomId) {

        if (roomId == null) {

            throw new IllegalArgumentException(
                    "Room id is required"
            );
        }

        return roomRepository.findById(roomId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Room not found with id: " + roomId
                        )
                );
    }


    // =========================
    // GET EMPLOYEE
    // =========================

    private Employee getEmployee(
            Long employeeId) {

        if (employeeId == null) {

            throw new IllegalArgumentException(
                    "Employee id is required"
            );
        }

        return employeeRepository
                .findById(employeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found with id: "
                                        + employeeId
                        )
                );
    }


    // =========================
    // CONFLICT DETECTION
    // =========================

    private void checkConflict(
            Long roomId,
            LocalDateTime start,
            LocalDateTime end,
            Long currentBookingId) {

        List<Booking> confirmed =
                bookingRepository
                        .findByRoomIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                                roomId,
                                "CONFIRMED",
                                end,
                                start
                        );

        List<Booking> checkedIn =
                bookingRepository
                        .findByRoomIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                                roomId,
                                "CHECKED_IN",
                                end,
                                start
                        );

        confirmed.addAll(checkedIn);

        boolean conflict =
                confirmed.stream()
                        .anyMatch(booking ->
                                currentBookingId == null
                                        ||
                                        !booking.getId()
                                                .equals(currentBookingId)
                        );

        if (conflict) {

            throw new BookingConflictException(
                    "Room is already booked for the selected time slot"
            );
        }
    }


    // =========================
    // AUTO RELEASE
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

            System.out.println(
                    "Booking #" +
                            booking.getId() +
                            " automatically released due to no-show."
            );
        }
    }
}