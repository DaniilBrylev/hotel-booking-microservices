package com.example.booking.config;

import com.example.booking.model.Role;
import com.example.booking.model.User;
import com.example.booking.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public void run(String... args) {
    if (userRepository.findByUsername("admin").isEmpty()) {
      User admin = new User("admin", passwordEncoder.encode("adminpass"), Role.ADMIN.name());
      userRepository.save(admin);
    }
    if (userRepository.findByUsername("user").isEmpty()) {
      User user = new User("user", passwordEncoder.encode("userpass"), Role.USER.name());
      userRepository.save(user);
    }
  }
}
