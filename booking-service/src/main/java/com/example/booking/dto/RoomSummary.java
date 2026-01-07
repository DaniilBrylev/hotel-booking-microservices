package com.example.booking.dto;

public class RoomSummary {
  private Long id;
  private Long hotelId;
  private String number;
  private boolean available;
  private int timesBooked;

  public RoomSummary() {
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
