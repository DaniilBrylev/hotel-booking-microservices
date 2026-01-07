package com.example.hotel.config;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class InternalTokenFilter extends OncePerRequestFilter {
  private static final String INTERNAL_HEADER = "X-Internal-Token";

  @Value("${internal.token}")
  private String expectedToken;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String path = request.getRequestURI();
    if (isInternalPath(path)) {
      String actual = request.getHeader(INTERNAL_HEADER);
      if (actual == null || !actual.equals(expectedToken)) {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        return;
      }
    }
    filterChain.doFilter(request, response);
  }

  private boolean isInternalPath(String path) {
    if (path == null) {
      return false;
    }
    return path.startsWith("/api/rooms/")
        && (path.endsWith("/confirm-availability") || path.endsWith("/release"));
  }
}
