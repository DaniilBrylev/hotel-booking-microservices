package com.example.booking.controller;

import com.example.booking.dto.AuthRequest;
import com.example.booking.dto.AuthResponse;
import com.example.booking.dto.RegisterRequest;
import com.example.booking.dto.UserCreateRequest;
import com.example.booking.dto.UserResponse;
import com.example.booking.dto.UserUpdateRequest;
import com.example.booking.service.AuthService;
import com.example.booking.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {
  private final UserService userService;
  private final AuthService authService;

  public UserController(UserService userService, AuthService authService) {
    this.userService = userService;
    this.authService = authService;
  }

  @PostMapping("/register")
  public UserResponse register(@Valid @RequestBody RegisterRequest request) {
    return userService.register(request);
  }

  @PostMapping("/auth")
  public AuthResponse authenticate(@Valid @RequestBody AuthRequest request) {
    return authService.authenticate(request);
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public UserResponse createUser(@Valid @RequestBody UserCreateRequest request) {
    return userService.createUser(request);
  }

  @PatchMapping
  @PreAuthorize("hasRole('ADMIN')")
  public UserResponse updateUser(@Valid @RequestBody UserUpdateRequest request) {
    return userService.updateUser(request);
  }

  @DeleteMapping
  @PreAuthorize("hasRole('ADMIN')")
  public void deleteUser(@RequestParam Long id) {
    userService.deleteUser(id);
  }
}
