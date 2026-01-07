package com.example.hotel.controller;

import com.example.hotel.dto.RoomRequest;
import com.example.hotel.dto.RoomResponse;
import com.example.hotel.service.RoomService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
  private final RoomService roomService;

  public RoomController(RoomService roomService) {
    this.roomService = roomService;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public List<RoomResponse> getRooms(@RequestParam(required = false) Long hotelId) {
    return roomService.getRooms(hotelId);
  }

  @GetMapping("/recommend")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public List<RoomResponse> recommendRooms(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    return roomService.recommendRooms(startDate, endDate);
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public RoomResponse createRoom(@Valid @RequestBody RoomRequest request) {
    return roomService.createRoom(request);
  }
}
