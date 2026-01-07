package com.example.hotel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RoomRequest {
  @NotNull
  private Long hotelId;

  @NotBlank
  private String number;

  private Boolean available;

  private Integer timesBooked;

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

  public Boolean getAvailable() {
    return available;
  }

  public void setAvailable(Boolean available) {
    this.available = available;
  }

  public Integer getTimesBooked() {
    return timesBooked;
  }

  public void setTimesBooked(Integer timesBooked) {
    this.timesBooked = timesBooked;
  }
}
