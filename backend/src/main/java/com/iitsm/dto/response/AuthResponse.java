package com.iitsm.dto.response;

public record AuthResponse(String token, String email, String role, String fullName) {}
