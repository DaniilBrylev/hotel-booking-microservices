package com.example.booking.exception;

public class RemoteCallException extends RuntimeException {
  private final boolean retryable;

  public RemoteCallException(String message, boolean retryable) {
    super(message);
    this.retryable = retryable;
  }

  public boolean isRetryable() {
    return retryable;
  }
}
