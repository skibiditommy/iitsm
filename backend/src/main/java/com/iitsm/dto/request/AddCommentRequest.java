package com.iitsm.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddCommentRequest(
        @NotBlank(message = "Content is required") String content,
        boolean internal
) {}
