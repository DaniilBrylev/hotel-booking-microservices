package com.example.booking.service;

import com.example.booking.dto.RegisterRequest;
import com.example.booking.dto.UserCreateRequest;
import com.example.booking.dto.UserResponse;
import com.example.booking.dto.UserUpdateRequest;
import com.example.booking.exception.BadRequestException;
import com.example.booking.exception.NotFoundException;
import com.example.booking.model.Role;
import com.example.booking.model.User;
import com.example.booking.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public UserResponse register(RegisterRequest request) {
    if (userRepository.findByUsername(request.getUsername()).isPresent()) {
      throw new BadRequestException("Username already exists");
    }
    User user = new User(request.getUsername(), passwordEncoder.encode(request.getPassword()), Role.USER.name());
    return toResponse(userRepository.save(user));
  }

  @Transactional
  public UserResponse createUser(UserCreateRequest request) {
    if (userRepository.findByUsername(request.getUsername()).isPresent()) {
      throw new BadRequestException("Username already exists");
    }
    User user = new User(request.getUsername(), passwordEncoder.encode(request.getPassword()), request.getRole().name());
    return toResponse(userRepository.save(user));
  }

  @Transactional
  public UserResponse updateUser(UserUpdateRequest request) {
    User user = userRepository.findById(request.getId())
        .orElseThrow(() -> new NotFoundException("User not found"));
    if (request.getPassword() != null && !request.getPassword().isBlank()) {
      user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    }
    if (request.getRole() != null) {
      user.setRole(request.getRole().name());
    }
    return toResponse(userRepository.save(user));
  }

  @Transactional
  public void deleteUser(Long id) {
    if (!userRepository.existsById(id)) {
      throw new NotFoundException("User not found");
    }
    userRepository.deleteById(id);
  }

  @Transactional(readOnly = true)
  public User getByUsername(String username) {
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new NotFoundException("User not found"));
  }

  private UserResponse toResponse(User user) {
    return new UserResponse(user.getId(), user.getUsername(), user.getRole());
  }
}
