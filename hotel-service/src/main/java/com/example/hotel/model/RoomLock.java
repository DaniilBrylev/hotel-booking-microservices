package com.example.hotel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "room_locks",
    uniqueConstraints = @UniqueConstraint(name = "uk_room_locks_request_id", columnNames = "request_id"),
    indexes = {
        @Index(name = "idx_room_locks_room", columnList = "room_id"),
        @Index(name = "idx_room_locks_status", columnList = "status")
    })
public class RoomLock {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "room_id", nullable = false)
  private Long roomId;

  @Column(nullable = false)
  private LocalDate startDate;

  @Column(nullable = false)
  private LocalDate endDate;

  @Column(name = "request_id", nullable = false, unique = true)
  private String requestId;

  @Column(name = "booking_id", nullable = false)
  private Long bookingId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private RoomLockStatus status;

  @Column(nullable = false)
  private Instant createdAt;

  public RoomLock() {
  }

  public RoomLock(Long roomId, LocalDate startDate, LocalDate endDate, String requestId,
                  Long bookingId, RoomLockStatus status, Instant createdAt) {
    this.roomId = roomId;
    this.startDate = startDate;
    this.endDate = endDate;
    this.requestId = requestId;
    this.bookingId = bookingId;
    this.status = status;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public RoomLockStatus getStatus() {
    return status;
  }

  public void setStatus(RoomLockStatus status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
