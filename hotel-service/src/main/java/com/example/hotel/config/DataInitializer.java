package com.example.hotel.config;

import com.example.hotel.model.Hotel;
import com.example.hotel.model.Room;
import com.example.hotel.repository.HotelRepository;
import com.example.hotel.repository.RoomRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
  private final HotelRepository hotelRepository;
  private final RoomRepository roomRepository;

  public DataInitializer(HotelRepository hotelRepository, RoomRepository roomRepository) {
    this.hotelRepository = hotelRepository;
    this.roomRepository = roomRepository;
  }

  @Override
  public void run(String... args) {
    if (hotelRepository.count() > 0) {
      return;
    }

    Hotel first = hotelRepository.save(new Hotel("Sunrise Hotel", "1 Ocean Ave"));
    Hotel second = hotelRepository.save(new Hotel("Mountain View", "99 Alpine Rd"));

    List<Room> rooms = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      rooms.add(new Room(first.getId(), "10" + i, true, i - 1));
    }
    for (int i = 1; i <= 5; i++) {
      rooms.add(new Room(second.getId(), "20" + i, true, 5 - i));
    }
    roomRepository.saveAll(rooms);
  }
}
