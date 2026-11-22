package com.iitsm.dto.response;

import java.time.LocalDateTime;

public record ErrorResponse(String type, String title, int status, String detail,
                              String instance, LocalDateTime timestamp) {
    public ErrorResponse(String type, String title, int status, String detail, String instance) {
        this(type, title, status, detail, instance, LocalDateTime.now());
    }
}
