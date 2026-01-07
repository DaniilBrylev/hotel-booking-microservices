package com.example.booking.repository;

import com.example.booking.model.Booking;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
  List<Booking> findByUserId(Long userId);

  Optional<Booking> findByRequestId(String requestId);
}
