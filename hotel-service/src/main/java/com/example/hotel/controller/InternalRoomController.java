package com.example.hotel.controller;

import com.example.hotel.dto.AvailabilityRequest;
import com.example.hotel.dto.RoomLockResponse;
import com.example.hotel.service.RoomLockService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class InternalRoomController {
  private final RoomLockService roomLockService;

  public InternalRoomController(RoomLockService roomLockService) {
    this.roomLockService = roomLockService;
  }

  @PostMapping("/{id}/confirm-availability")
  public RoomLockResponse confirmAvailability(@PathVariable("id") Long roomId,
                                              @Valid @RequestBody AvailabilityRequest request) {
    return roomLockService.confirmAvailability(roomId, request);
  }

  @PostMapping("/{id}/release")
  public RoomLockResponse release(@PathVariable("id") Long roomId,
                                  @Valid @RequestBody AvailabilityRequest request) {
    return roomLockService.release(roomId, request);
  }
}
