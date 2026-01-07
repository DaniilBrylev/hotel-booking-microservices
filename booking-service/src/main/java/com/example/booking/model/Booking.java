package com.example.booking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "bookings", indexes = {
    @Index(name = "idx_bookings_user", columnList = "user_id"),
    @Index(name = "idx_bookings_room", columnList = "room_id"),
    @Index(name = "idx_bookings_request", columnList = "request_id", unique = true)
})
public class Booking {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "room_id")
  private Long roomId;

  @Column(name = "request_id", nullable = false, unique = true)
  private String requestId;

  @Column(nullable = false)
  private LocalDate startDate;

  @Column(nullable = false)
  private LocalDate endDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BookingStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "failure_reason")
  private BookingFailureReason failureReason;

  @Column(nullable = false)
  private Instant createdAt;

  public Booking() {
  }

  public Booking(Long userId, Long roomId, LocalDate startDate, LocalDate endDate,
                 BookingStatus status, String requestId) {
    this.userId = userId;
    this.roomId = roomId;
    this.startDate = startDate;
    this.endDate = endDate;
    this.status = status;
    this.requestId = requestId;
  }

  @PrePersist
  void onCreate() {
    this.createdAt = Instant.now();
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

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
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

  public BookingStatus getStatus() {
    return status;
  }

  public void setStatus(BookingStatus status) {
    this.status = status;
  }

  public BookingFailureReason getFailureReason() {
    return failureReason;
  }

  public void setFailureReason(BookingFailureReason failureReason) {
    this.failureReason = failureReason;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
