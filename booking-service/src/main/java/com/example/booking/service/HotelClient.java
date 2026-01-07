package com.example.booking.service;

import com.example.booking.dto.AvailabilityRequest;
import com.example.booking.dto.RoomLockResponse;
import com.example.booking.dto.RoomSummary;
import com.example.booking.exception.RemoteCallException;
import com.example.booking.exception.RoomConflictException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
public class HotelClient {
  private final WebClient webClient;
  private final String internalToken;
  private final Duration timeout;
  private final int maxAttempts;
  private final Duration backoff;

  public HotelClient(WebClient.Builder webClientBuilder,
                     @Value("${booking.hotel.base-url}") String baseUrl,
                     @Value("${booking.hotel.internal-token}") String internalToken,
                     @Value("${booking.hotel.timeout}") Duration timeout,
                     @Value("${booking.hotel.retry.max-attempts}") int maxAttempts,
                     @Value("${booking.hotel.retry.backoff}") Duration backoff) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    this.internalToken = internalToken;
    this.timeout = timeout;
    this.maxAttempts = maxAttempts;
    this.backoff = backoff;
  }

  public RoomLockResponse confirmAvailability(Long roomId, AvailabilityRequest request) {
    return executeWithRetry(() -> webClient.post()
        .uri("/api/rooms/{id}/confirm-availability", roomId)
        .header("X-Internal-Token", internalToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .retrieve()
        .onStatus(status -> status.value() == 409,
            response -> Mono.error(new RoomConflictException("Room is already booked")))
        .onStatus(status -> status.is4xxClientError(),
            response -> Mono.error(new RemoteCallException("Hotel service rejected request", false)))
        .onStatus(status -> status.is5xxServerError(),
            response -> Mono.error(new RemoteCallException("Hotel service unavailable", true)))
        .bodyToMono(RoomLockResponse.class));
  }

  public RoomLockResponse release(Long roomId, AvailabilityRequest request) {
    return executeWithRetry(() -> webClient.post()
        .uri("/api/rooms/{id}/release", roomId)
        .header("X-Internal-Token", internalToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .retrieve()
        .onStatus(status -> status.is4xxClientError(),
            response -> Mono.error(new RemoteCallException("Hotel service rejected request", false)))
        .onStatus(status -> status.is5xxServerError(),
            response -> Mono.error(new RemoteCallException("Hotel service unavailable", true)))
        .bodyToMono(RoomLockResponse.class));
  }

  public List<RoomSummary> recommendRooms(LocalDate startDate, LocalDate endDate) {
    return executeWithRetry(() -> webClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/api/rooms/recommend")
            .queryParam("startDate", startDate)
            .queryParam("endDate", endDate)
            .build())
        .retrieve()
        .onStatus(status -> status.is4xxClientError(),
            response -> Mono.error(new RemoteCallException("Hotel service rejected request", false)))
        .onStatus(status -> status.is5xxServerError(),
            response -> Mono.error(new RemoteCallException("Hotel service unavailable", true)))
        .bodyToMono(new ParameterizedTypeReference<List<RoomSummary>>() {}));
  }

  private <T> T executeWithRetry(Supplier<Mono<T>> supplier) {
    Mono<T> mono = supplier.get().timeout(timeout);
    int retries = Math.max(0, maxAttempts - 1);
    if (retries > 0) {
      mono = mono.retryWhen(Retry.backoff(retries, backoff)
          .filter(this::isRetryable)
          .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
    }
    return mono.block();
  }

  private boolean isRetryable(Throwable throwable) {
    if (throwable instanceof RoomConflictException) {
      return false;
    }
    if (throwable instanceof RemoteCallException remote) {
      return remote.isRetryable();
    }
    return throwable instanceof WebClientRequestException || throwable instanceof java.util.concurrent.TimeoutException;
  }
}
