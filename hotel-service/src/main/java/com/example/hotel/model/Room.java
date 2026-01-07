package com.example.hotel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "rooms", indexes = {
    @Index(name = "idx_rooms_hotel", columnList = "hotel_id")
})
public class Room {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "hotel_id", nullable = false)
  private Long hotelId;

  @Column(nullable = false)
  private String number;

  @Column(nullable = false)
  private boolean available = true;

  @Column(nullable = false)
  private int timesBooked = 0;

  public Room() {
  }

  public Room(Long hotelId, String number, boolean available, int timesBooked) {
    this.hotelId = hotelId;
    this.number = number;
    this.available = available;
    this.timesBooked = timesBooked;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getHotelId() {
    return hotelId;
  }

  public void setHotelId(Long hotelId) {
    this.hotelId = hotelId;
  }

  public String getNumber() {
    return number;
  }

  public void setNumber(String number) {
    this.number = number;
  }

  public boolean isAvailable() {
    return available;
  }

  public void setAvailable(boolean available) {
    this.available = available;
  }

  public int getTimesBooked() {
    return timesBooked;
  }

  public void setTimesBooked(int timesBooked) {
    this.timesBooked = timesBooked;
  }
}
