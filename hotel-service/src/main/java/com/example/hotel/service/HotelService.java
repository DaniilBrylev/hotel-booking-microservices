package com.example.hotel.service;

import com.example.hotel.dto.HotelRequest;
import com.example.hotel.exception.BadRequestException;
import com.example.hotel.model.Hotel;
import com.example.hotel.repository.HotelRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HotelService {
  private final HotelRepository hotelRepository;

  public HotelService(HotelRepository hotelRepository) {
    this.hotelRepository = hotelRepository;
  }

  @Transactional
  public Hotel createHotel(HotelRequest request) {
    if (request.getName() == null || request.getName().isBlank()) {
      throw new BadRequestException("Hotel name is required");
    }
    if (request.getAddress() == null || request.getAddress().isBlank()) {
      throw new BadRequestException("Hotel address is required");
    }
    Hotel hotel = new Hotel(request.getName(), request.getAddress());
    return hotelRepository.save(hotel);
  }

  @Transactional(readOnly = true)
  public List<Hotel> getHotels() {
    return hotelRepository.findAll();
  }
}
