package com.example.hotel.service;

import com.example.hotel.dto.RoomRequest;
import com.example.hotel.dto.RoomResponse;
import com.example.hotel.exception.BadRequestException;
import com.example.hotel.model.Room;
import com.example.hotel.model.RoomLockStatus;
import com.example.hotel.repository.HotelRepository;
import com.example.hotel.repository.RoomRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {
  private final RoomRepository roomRepository;
  private final HotelRepository hotelRepository;

  public RoomService(RoomRepository roomRepository, HotelRepository hotelRepository) {
    this.roomRepository = roomRepository;
    this.hotelRepository = hotelRepository;
  }

  @Transactional
  public RoomResponse createRoom(RoomRequest request) {
    if (request.getHotelId() == null) {
      throw new BadRequestException("Hotel id is required");
    }
    if (!hotelRepository.existsById(request.getHotelId())) {
      throw new BadRequestException("Hotel not found");
    }
    Room room = new Room(
        request.getHotelId(),
        request.getNumber(),
        request.getAvailable() == null ? true : request.getAvailable(),
        request.getTimesBooked() == null ? 0 : request.getTimesBooked());
    Room saved = roomRepository.save(room);
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<RoomResponse> getRooms(Long hotelId) {
    List<Room> rooms = hotelId == null ? roomRepository.findAll() : roomRepository.findByHotelId(hotelId);
    return rooms.stream().map(this::toResponse).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<RoomResponse> recommendRooms(LocalDate startDate, LocalDate endDate) {
    validateDates(startDate, endDate);
    List<Room> rooms = roomRepository.findRecommendations(startDate, endDate, RoomLockStatus.LOCKED);
    return rooms.stream().map(this::toResponse).collect(Collectors.toList());
  }

  private void validateDates(LocalDate startDate, LocalDate endDate) {
    if (startDate == null || endDate == null) {
      throw new BadRequestException("startDate and endDate are required");
    }
    if (endDate.isBefore(startDate)) {
      throw new BadRequestException("endDate must be after startDate");
    }
  }

  private RoomResponse toResponse(Room room) {
    return new RoomResponse(room.getId(), room.getHotelId(), room.getNumber(), room.isAvailable(), room.getTimesBooked());
  }
}
