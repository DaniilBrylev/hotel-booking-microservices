package com.example.booking.service;

import com.example.booking.dto.AuthRequest;
import com.example.booking.dto.AuthResponse;
import com.example.booking.exception.UnauthorizedException;
import com.example.booking.model.User;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private final UserService userService;
  private final PasswordEncoder passwordEncoder;
  private final JwtEncoder jwtEncoder;
  private final Duration ttl;

  public AuthService(UserService userService,
                     PasswordEncoder passwordEncoder,
                     JwtEncoder jwtEncoder,
                     @Value("${security.jwt.ttl}") Duration ttl) {
    this.userService = userService;
    this.passwordEncoder = passwordEncoder;
    this.jwtEncoder = jwtEncoder;
    this.ttl = ttl;
  }

  public AuthResponse authenticate(AuthRequest request) {
    User user = userService.getByUsername(request.getUsername());
    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new UnauthorizedException("Invalid credentials");
    }

    Instant now = Instant.now();
    Instant expiresAt = now.plus(ttl);

    JwtClaimsSet claims = JwtClaimsSet.builder()
        .subject(user.getUsername())
        .issuedAt(now)
        .expiresAt(expiresAt)
        .claim("roles", List.of(user.getRole()))
        .claim("userId", user.getId())
        .build();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new AuthResponse(token, expiresAt);
  }
}
