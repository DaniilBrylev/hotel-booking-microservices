package com.example.booking.service;

import com.example.booking.dto.AvailabilityRequest;
import com.example.booking.dto.BookingRequest;
import com.example.booking.dto.BookingResponse;
import com.example.booking.dto.RoomSummary;
import com.example.booking.exception.BadRequestException;
import com.example.booking.exception.NotFoundException;
import com.example.booking.exception.RemoteCallException;
import com.example.booking.exception.RoomConflictException;
import com.example.booking.model.Booking;
import com.example.booking.model.BookingFailureReason;
import com.example.booking.model.BookingStatus;
import com.example.booking.repository.BookingRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {
  private static final Logger log = LoggerFactory.getLogger(BookingService.class);

  private final BookingRepository bookingRepository;
  private final HotelClient hotelClient;
  private final CurrentUserProvider currentUserProvider;

  public BookingService(BookingRepository bookingRepository,
                        HotelClient hotelClient,
                        CurrentUserProvider currentUserProvider) {
    this.bookingRepository = bookingRepository;
    this.hotelClient = hotelClient;
    this.currentUserProvider = currentUserProvider;
  }

  @Transactional(noRollbackFor = {RoomConflictException.class, RemoteCallException.class})
  public BookingResponse createBooking(BookingRequest request) {
    validateDates(request.getStartDate(), request.getEndDate());
    Long userId = currentUserProvider.getUserId();
    String bookingRequestId = requireRequestId(request.getRequestId());

    Booking existing = bookingRepository.findByRequestId(bookingRequestId).orElse(null);
    if (existing != null) {
      if (!existing.getUserId().equals(userId)) {
        throw new NotFoundException("Booking not found");
      }
      if (existing.getStatus() == BookingStatus.CANCELLED) {
        BookingFailureReason reason = existing.getFailureReason();
        if (reason == BookingFailureReason.CONFLICT) {
          throw new RoomConflictException("Booking already failed due to conflict");
        }
        if (reason == BookingFailureReason.REMOTE_ERROR) {
          throw new RemoteCallException("Booking already failed due to hotel service error", false);
        }
      }
      return toResponse(existing);
    }

    boolean autoSelect = Boolean.TRUE.equals(request.getAutoSelect());
    log.info("Booking request received requestId={}, userId={}, autoSelect={}, roomId={}, startDate={}, endDate={}",
        bookingRequestId, userId, autoSelect, request.getRoomId(), request.getStartDate(), request.getEndDate());

    if (autoSelect) {
      return handleAutoSelect(request, userId, bookingRequestId);
    }

    if (request.getRoomId() == null) {
      throw new BadRequestException("roomId is required when autoSelect is false");
    }

    Booking booking = new Booking(userId, request.getRoomId(), request.getStartDate(),
        request.getEndDate(), BookingStatus.PENDING, bookingRequestId);
    booking = saveBooking(booking);
    log.info("Booking created bookingId={}, requestId={}, status={}", booking.getId(), bookingRequestId, booking.getStatus());
    return handleManualRoomSelection(booking, request.getRoomId());
  }

  @Transactional(readOnly = true)
  public List<BookingResponse> getBookingsForCurrentUser() {
    Long userId = currentUserProvider.getUserId();
    return bookingRepository.findByUserId(userId).stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public BookingResponse getBooking(Long id) {
    Booking booking = bookingRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Booking not found"));
    verifyOwner(booking);
    return toResponse(booking);
  }

  @Transactional
  public BookingResponse cancelBooking(Long id) {
    Booking booking = bookingRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Booking not found"));
    verifyOwner(booking);

    if (booking.getStatus() == BookingStatus.CANCELLED) {
      return toResponse(booking);
    }

    booking.setStatus(BookingStatus.CANCELLED);
    booking.setFailureReason(BookingFailureReason.USER_CANCELLED);
    bookingRepository.save(booking);

    if (booking.getRoomId() != null) {
      String requestId = UUID.randomUUID().toString();
      AvailabilityRequest releaseRequest = new AvailabilityRequest(
          booking.getStartDate(), booking.getEndDate(), requestId, booking.getId());
      try {
        log.info("Cancel release bookingId={}, requestId={}, roomId={}", booking.getId(), requestId, booking.getRoomId());
        hotelClient.release(booking.getRoomId(), releaseRequest);
      } catch (Exception ex) {
        log.warn("Release failed for bookingId={}, requestId={}", booking.getId(), requestId, ex);
      }
    }

    return toResponse(booking);
  }

  private BookingResponse handleManualRoomSelection(Booking booking, Long roomId) {
    String confirmRequestId = UUID.randomUUID().toString();
    AvailabilityRequest availabilityRequest = new AvailabilityRequest(
        booking.getStartDate(), booking.getEndDate(), confirmRequestId, booking.getId());

    log.info("Booking attempt bookingId={}, requestId={}, confirmRequestId={}, roomId={}, startDate={}, endDate={}",
        booking.getId(), booking.getRequestId(), confirmRequestId, roomId, booking.getStartDate(), booking.getEndDate());

    try {
      hotelClient.confirmAvailability(roomId, availabilityRequest);
      booking.setStatus(BookingStatus.CONFIRMED);
      booking.setRoomId(roomId);
      bookingRepository.save(booking);
      log.info("Booking confirmed bookingId={}, requestId={}, confirmRequestId={}, roomId={}",
          booking.getId(), booking.getRequestId(), confirmRequestId, roomId);
      return toResponse(booking);
    } catch (RoomConflictException ex) {
      booking.setStatus(BookingStatus.CANCELLED);
      booking.setFailureReason(BookingFailureReason.CONFLICT);
      bookingRepository.save(booking);
      log.info("Booking conflict bookingId={}, requestId={}, confirmRequestId={}, roomId={}",
          booking.getId(), booking.getRequestId(), confirmRequestId, roomId);
      throw ex;
    } catch (Exception ex) {
      compensate(roomId, booking, confirmRequestId, ex);
      if (ex instanceof RemoteCallException remote) {
        throw remote;
      }
      throw new RemoteCallException("Hotel service error during booking", true);
    }
  }

  private BookingResponse handleAutoSelect(BookingRequest request, Long userId, String bookingRequestId) {
    List<RoomSummary> recommended;
    try {
      recommended = hotelClient.recommendRooms(request.getStartDate(), request.getEndDate());
    } catch (Exception ex) {
      if (ex instanceof RemoteCallException remote) {
        throw remote;
      }
      throw new RemoteCallException("Hotel service error during recommendations", true);
    }
    if (recommended.isEmpty()) {
      throw new RoomConflictException("No available rooms for selected dates");
    }

    Booking booking = new Booking(userId, recommended.get(0).getId(),
        request.getStartDate(), request.getEndDate(), BookingStatus.PENDING, bookingRequestId);
    booking = saveBooking(booking);
    log.info("Booking created bookingId={}, requestId={}, status={}", booking.getId(), bookingRequestId, booking.getStatus());
    int attempts = 0;

    for (RoomSummary room : recommended) {
      if (attempts >= 5) {
        break;
      }
      attempts++;
      booking.setRoomId(room.getId());
      String confirmRequestId = UUID.randomUUID().toString();
      AvailabilityRequest availabilityRequest = new AvailabilityRequest(
          request.getStartDate(), request.getEndDate(), confirmRequestId, booking.getId());

      log.info("Auto-select attempt bookingId={}, requestId={}, confirmRequestId={}, roomId={}, startDate={}, endDate={}",
          booking.getId(), bookingRequestId, confirmRequestId, room.getId(),
          request.getStartDate(), request.getEndDate());

      try {
        hotelClient.confirmAvailability(room.getId(), availabilityRequest);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setRoomId(room.getId());
        bookingRepository.save(booking);
        log.info("Booking confirmed bookingId={}, requestId={}, confirmRequestId={}, roomId={}",
            booking.getId(), bookingRequestId, confirmRequestId, room.getId());
        return toResponse(booking);
      } catch (RoomConflictException ex) {
        log.info("Room conflict bookingId={}, requestId={}, confirmRequestId={}, roomId={}",
            booking.getId(), bookingRequestId, confirmRequestId, room.getId());
      } catch (Exception ex) {
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setFailureReason(BookingFailureReason.REMOTE_ERROR);
        bookingRepository.save(booking);
        releaseSilently(room.getId(), booking, confirmRequestId, ex);
        if (ex instanceof RemoteCallException remote) {
          throw remote;
        }
        throw new RemoteCallException("Hotel service error during booking", true);
      }
    }

    booking.setStatus(BookingStatus.CANCELLED);
    booking.setFailureReason(BookingFailureReason.CONFLICT);
    bookingRepository.save(booking);
    throw new RoomConflictException("No rooms available for selected dates");
  }

  private void releaseSilently(Long roomId, Booking booking, String requestId, Exception cause) {
    log.warn("Auto-select compensation bookingId={}, requestId={}, confirmRequestId={}, roomId={} due to error",
        booking.getId(), booking.getRequestId(), requestId, roomId, cause);
    try {
      AvailabilityRequest releaseRequest = new AvailabilityRequest(
          booking.getStartDate(), booking.getEndDate(), requestId, booking.getId());
      hotelClient.release(roomId, releaseRequest);
      log.info("Auto-select release completed bookingId={}, requestId={}, confirmRequestId={}, roomId={}",
          booking.getId(), booking.getRequestId(), requestId, roomId);
    } catch (Exception ex) {
      log.warn("Auto-select release failed bookingId={}, requestId={}, confirmRequestId={}, roomId={}",
          booking.getId(), booking.getRequestId(), requestId, roomId, ex);
    }
  }

  private void compensate(Long roomId, Booking booking, String requestId, Exception cause) {
    booking.setStatus(BookingStatus.CANCELLED);
    booking.setFailureReason(BookingFailureReason.REMOTE_ERROR);
    bookingRepository.save(booking);
    log.warn("Compensating bookingId={}, requestId={}, confirmRequestId={}, roomId={} due to error",
        booking.getId(), booking.getRequestId(), requestId, roomId, cause);

    try {
      AvailabilityRequest releaseRequest = new AvailabilityRequest(
          booking.getStartDate(), booking.getEndDate(), requestId, booking.getId());
      hotelClient.release(roomId, releaseRequest);
      log.info("Compensation completed bookingId={}, requestId={}, confirmRequestId={}, roomId={}",
          booking.getId(), booking.getRequestId(), requestId, roomId);
    } catch (Exception ex) {
      log.warn("Compensation failed bookingId={}, requestId={}, confirmRequestId={}, roomId={}",
          booking.getId(), booking.getRequestId(), requestId, roomId, ex);
    }
  }

  private void validateDates(LocalDate startDate, LocalDate endDate) {
    if (startDate == null || endDate == null) {
      throw new BadRequestException("startDate and endDate are required");
    }
    if (endDate.isBefore(startDate)) {
      throw new BadRequestException("endDate must be after startDate");
    }
  }

  private void verifyOwner(Booking booking) {
    Long userId = currentUserProvider.getUserId();
    if (!booking.getUserId().equals(userId)) {
      throw new NotFoundException("Booking not found");
    }
  }

  private BookingResponse toResponse(Booking booking) {
    return new BookingResponse(
        booking.getId(),
        booking.getUserId(),
        booking.getRoomId(),
        booking.getStartDate(),
        booking.getEndDate(),
        booking.getStatus().name(),
        booking.getCreatedAt());
  }

  private Booking saveBooking(Booking booking) {
    try {
      return bookingRepository.save(booking);
    } catch (DataIntegrityViolationException ex) {
      Booking existing = bookingRepository.findByRequestId(booking.getRequestId()).orElse(null);
      if (existing != null) {
        return existing;
      }
      throw ex;
    }
  }

  private String requireRequestId(String requestId) {
    if (requestId == null || requestId.isBlank()) {
      throw new BadRequestException("requestId is required");
    }
    return requestId;
  }
}
