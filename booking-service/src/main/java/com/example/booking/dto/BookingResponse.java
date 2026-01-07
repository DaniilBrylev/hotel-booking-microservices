package com.example.booking.dto;

import java.time.Instant;
import java.time.LocalDate;

public class BookingResponse {
  private Long id;
  private Long userId;
  private Long roomId;
  private LocalDate startDate;
  private LocalDate endDate;
  private String status;
  private Instant createdAt;

  public BookingResponse() {
  }

  public BookingResponse(Long id, Long userId, Long roomId, LocalDate startDate,
                         LocalDate endDate, String status, Instant createdAt) {
    this.id = id;
    this.userId = userId;
    this.roomId = roomId;
    this.startDate = startDate;
    this.endDate = endDate;
    this.status = status;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getRoomId() {
    return roomId;
  }

  public void setRoomId(Long roomId) {
    this.roomId = roomId;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
