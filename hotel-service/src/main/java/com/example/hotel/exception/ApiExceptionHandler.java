package com.example.hotel.exception;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(RoomConflictException.class)
  public ResponseEntity<ErrorResponse> handleConflict(RoomConflictException ex) {
    return build(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
    return build(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    return build(HttpStatus.BAD_REQUEST, "Validation failed");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
  }

  private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
    return ResponseEntity.status(status)
        .body(new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message));
  }

  public static class ErrorResponse {
    private Instant timestamp;
    private int status;
    private String error;
    private String message;

    public ErrorResponse(Instant timestamp, int status, String error, String message) {
      this.timestamp = timestamp;
      this.status = status;
      this.error = error;
      this.message = message;
    }

    public Instant getTimestamp() {
      return timestamp;
    }

    public int getStatus() {
      return status;
    }

    public String getError() {
      return error;
    }

    public String getMessage() {
      return message;
    }
  }
}
