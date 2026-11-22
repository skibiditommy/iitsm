package com.iitsm.dto.response;

import java.time.LocalDateTime;

public record CommentResponse(Long id, String content, String authorEmail,
                                String authorName, boolean internal, LocalDateTime createdAt) {}
