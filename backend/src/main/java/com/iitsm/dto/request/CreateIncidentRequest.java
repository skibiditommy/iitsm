package com.iitsm.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateIncidentRequest(
        @NotBlank(message = "Title is required") String title,
        String description,
        Long categoryId,
        Long affectedServiceId,
        @NotNull(message = "Impact is required") String impact,
        @NotNull(message = "Urgency is required") String urgency,
        @Min(value = 0) int affectedUsersCount,
        String attachmentName,
        String attachmentType,
        String attachmentData
) {
    public CreateIncidentRequest(String title, String description, Long categoryId, Long affectedServiceId, String impact, String urgency, int affectedUsersCount) {
        this(title, description, categoryId, affectedServiceId, impact, urgency, affectedUsersCount, null, null, null);
    }
}
