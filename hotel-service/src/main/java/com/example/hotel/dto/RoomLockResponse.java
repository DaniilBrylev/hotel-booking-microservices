package com.example.hotel.dto;

public class RoomLockResponse {
  private Long roomId;
  private Long bookingId;
  private String status;

  public RoomLockResponse() {
  }

  public RoomLockResponse(Long roomId, Long bookingId, String status) {
    this.roomId = roomId;
    this.bookingId = bookingId;
    this.status = status;
  }

  public Long getRoomId() {
    return roomId;
  }

  public void setRoomId(Long roomId) {
    this.roomId = roomId;
  }

  public Long getBookingId() {
    return bookingId;
  }

  public void setBookingId(Long bookingId) {
    this.bookingId = bookingId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
