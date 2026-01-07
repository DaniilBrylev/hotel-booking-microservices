package com.example.booking.service;

import com.example.booking.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

  public Long getUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      Object value = jwtAuth.getToken().getClaim("userId");
      if (value instanceof Number number) {
        return number.longValue();
      }
    }
    throw new UnauthorizedException("User is not authenticated");
  }

  public String getUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null) {
      return auth.getName();
    }
    throw new UnauthorizedException("User is not authenticated");
  }
}
