package com.example.roombook.repository;

import com.example.roombook.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking>
    findByRoomIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            Long roomId,
            String status,
            LocalDateTime endTime,
            LocalDateTime startTime
    );

    List<Booking>
    findByStatusAndCheckedInFalseAndStartTimeBefore(
            String status,
            LocalDateTime time
    );
}