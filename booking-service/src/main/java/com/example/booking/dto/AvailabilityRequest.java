package com.example.booking.dto;

import java.time.LocalDate;

public class AvailabilityRequest {
  private LocalDate startDate;
  private LocalDate endDate;
  private String requestId;
  private Long bookingId;

  public AvailabilityRequest() {
  }

  public AvailabilityRequest(LocalDate startDate, LocalDate endDate, String requestId, Long bookingId) {
    this.startDate = startDate;
    this.endDate = endDate;
    this.requestId = requestId;
    this.bookingId = bookingId;
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

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public Long getBookingId() {
    return bookingId;
  }

  public void setBookingId(Long bookingId) {
    this.bookingId = bookingId;
  }
}
