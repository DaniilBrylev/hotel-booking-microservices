package com.example.booking.dto;

import com.example.booking.model.Role;
import jakarta.validation.constraints.NotNull;

public class UserUpdateRequest {
  @NotNull
  private Long id;

  private String password;

  private Role role;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }
}
