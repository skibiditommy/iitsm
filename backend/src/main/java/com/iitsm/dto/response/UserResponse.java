package com.iitsm.dto.response;

public record UserResponse(Long id, String email, String firstName, String lastName,
                            String role, boolean active, String fullName) {}
