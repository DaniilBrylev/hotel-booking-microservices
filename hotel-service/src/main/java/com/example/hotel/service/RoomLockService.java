package com.example.hotel.service;

import com.example.hotel.dto.AvailabilityRequest;
import com.example.hotel.dto.RoomLockResponse;
import com.example.hotel.exception.BadRequestException;
import com.example.hotel.exception.NotFoundException;
import com.example.hotel.exception.RoomConflictException;
import com.example.hotel.model.Room;
import com.example.hotel.model.RoomLock;
import com.example.hotel.model.RoomLockStatus;
import com.example.hotel.repository.RoomLockRepository;
import com.example.hotel.repository.RoomRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomLockService {
  private static final Logger log = LoggerFactory.getLogger(RoomLockService.class);

  private final RoomRepository roomRepository;
  private final RoomLockRepository roomLockRepository;

  public RoomLockService(RoomRepository roomRepository, RoomLockRepository roomLockRepository) {
    this.roomRepository = roomRepository;
    this.roomLockRepository = roomLockRepository;
  }

  @Transactional
  public RoomLockResponse confirmAvailability(Long roomId, AvailabilityRequest request) {
    validateDates(request.getStartDate(), request.getEndDate());

    RoomLock existing = roomLockRepository.findByRequestId(request.getRequestId()).orElse(null);
    if (existing != null) {
      if (existing.getStatus() == RoomLockStatus.LOCKED) {
        log.info("Idempotent confirm for bookingId={}, requestId={}", request.getBookingId(), request.getRequestId());
        return toResponse(existing);
      }
      throw new RoomConflictException("Request already released");
    }

    Room room = roomRepository.findByIdForUpdate(roomId)
        .orElseThrow(() -> new NotFoundException("Room not found"));
    if (!room.isAvailable()) {
      throw new RoomConflictException("Room is not available");
    }

    boolean conflict = roomLockRepository.existsOverlappingLock(
        roomId, request.getStartDate(), request.getEndDate(), RoomLockStatus.LOCKED);
    if (conflict) {
      throw new RoomConflictException("Room is already booked for these dates");
    }

    RoomLock lock = new RoomLock(
        roomId,
        request.getStartDate(),
        request.getEndDate(),
        request.getRequestId(),
        request.getBookingId(),
        RoomLockStatus.LOCKED,
        Instant.now());
    RoomLock saved = roomLockRepository.save(lock);

    room.setTimesBooked(room.getTimesBooked() + 1);
    roomRepository.save(room);

    log.info("Room locked for bookingId={}, requestId={}, roomId={}",
        request.getBookingId(), request.getRequestId(), roomId);
    return toResponse(saved);
  }

  @Transactional
  public RoomLockResponse release(Long roomId, AvailabilityRequest request) {
    validateDates(request.getStartDate(), request.getEndDate());

    RoomLock existing = roomLockRepository.findByRequestId(request.getRequestId()).orElse(null);
    if (existing != null) {
      if (existing.getStatus() == RoomLockStatus.RELEASED) {
        log.info("Idempotent release for bookingId={}, requestId={}", request.getBookingId(), request.getRequestId());
        return toResponse(existing);
      }
      existing.setStatus(RoomLockStatus.RELEASED);
      RoomLock updated = roomLockRepository.save(existing);
      log.info("Room released for bookingId={}, requestId={}, roomId={}",
          request.getBookingId(), request.getRequestId(), roomId);
      return toResponse(updated);
    }

    RoomLock lockByBooking = roomLockRepository.findFirstByBookingIdAndStatus(
        request.getBookingId(), RoomLockStatus.LOCKED).orElse(null);
    if (lockByBooking != null) {
      lockByBooking.setStatus(RoomLockStatus.RELEASED);
      RoomLock updated = roomLockRepository.save(lockByBooking);
      log.info("Room released by bookingId={}, requestId={}, roomId={}",
          request.getBookingId(), request.getRequestId(), roomId);
      return toResponse(updated);
    }

    RoomLock marker = new RoomLock(
        roomId,
        request.getStartDate(),
        request.getEndDate(),
        request.getRequestId(),
        request.getBookingId(),
        RoomLockStatus.RELEASED,
        Instant.now());
    RoomLock saved = roomLockRepository.save(marker);
    log.info("Release marker created for bookingId={}, requestId={}, roomId={}",
        request.getBookingId(), request.getRequestId(), roomId);
    return toResponse(saved);
  }

  private void validateDates(LocalDate startDate, LocalDate endDate) {
    if (startDate == null || endDate == null) {
      throw new BadRequestException("startDate and endDate are required");
    }
    if (endDate.isBefore(startDate)) {
      throw new BadRequestException("endDate must be after startDate");
    }
  }

  private RoomLockResponse toResponse(RoomLock lock) {
    return new RoomLockResponse(lock.getRoomId(), lock.getBookingId(), lock.getStatus().name());
  }
}
