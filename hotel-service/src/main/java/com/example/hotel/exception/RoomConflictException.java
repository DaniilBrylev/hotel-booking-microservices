package com.example.hotel.exception;

public class RoomConflictException extends RuntimeException {
  public RoomConflictException(String message) {
    super(message);
  }
}
