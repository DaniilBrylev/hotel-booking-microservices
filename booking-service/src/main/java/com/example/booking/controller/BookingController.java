package com.example.booking.controller;

import com.example.booking.dto.BookingRequest;
import com.example.booking.dto.BookingResponse;
import com.example.booking.service.BookingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookingController {
  private final BookingService bookingService;

  public BookingController(BookingService bookingService) {
    this.bookingService = bookingService;
  }

  @PostMapping("/booking")
  @PreAuthorize("hasRole('USER')")
  public BookingResponse createBooking(@Valid @RequestBody BookingRequest request) {
    return bookingService.createBooking(request);
  }

  @GetMapping("/bookings")
  @PreAuthorize("hasRole('USER')")
  public List<BookingResponse> getBookings() {
    return bookingService.getBookingsForCurrentUser();
  }

  @GetMapping("/booking/{id}")
  @PreAuthorize("hasRole('USER')")
  public BookingResponse getBooking(@PathVariable Long id) {
    return bookingService.getBooking(id);
  }

  @DeleteMapping("/booking/{id}")
  @PreAuthorize("hasRole('USER')")
  public BookingResponse cancelBooking(@PathVariable Long id) {
    return bookingService.cancelBooking(id);
  }
}
