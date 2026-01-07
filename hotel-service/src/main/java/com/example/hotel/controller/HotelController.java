package com.example.hotel.controller;

import com.example.hotel.dto.HotelRequest;
import com.example.hotel.model.Hotel;
import com.example.hotel.service.HotelService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hotels")
public class HotelController {
  private final HotelService hotelService;

  public HotelController(HotelService hotelService) {
    this.hotelService = hotelService;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public List<Hotel> getHotels() {
    return hotelService.getHotels();
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public Hotel createHotel(@Valid @RequestBody HotelRequest request) {
    return hotelService.createHotel(request);
  }
}
