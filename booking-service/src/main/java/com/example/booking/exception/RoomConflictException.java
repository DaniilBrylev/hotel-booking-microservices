package com.example.booking.exception;

public class RoomConflictException extends RuntimeException {
  public RoomConflictException(String message) {
    super(message);
  }
}
