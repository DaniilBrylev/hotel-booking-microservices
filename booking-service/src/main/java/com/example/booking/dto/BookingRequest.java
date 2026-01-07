package com.example.booking.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class BookingRequest {
  @NotBlank
  private String requestId;

  @NotNull
  private LocalDate startDate;

  @NotNull
  private LocalDate endDate;

  private Long roomId;

  private Boolean autoSelect;

  @AssertTrue(message = "roomId is required when autoSelect is false")
  public boolean isRoomSelectionValid() {
    return Boolean.TRUE.equals(autoSelect) || roomId != null;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
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

  public Long getRoomId() {
    return roomId;
  }

  public void setRoomId(Long roomId) {
    this.roomId = roomId;
  }

  public Boolean getAutoSelect() {
    return autoSelect;
  }

  public void setAutoSelect(Boolean autoSelect) {
    this.autoSelect = autoSelect;
  }
}
