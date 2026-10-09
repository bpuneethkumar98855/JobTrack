package com.jobtrack.jobtrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StatusChangeRequest(
        @NotBlank(message = "Choose an application status.")
        @Size(max = 30, message = "Status must be 30 characters or fewer.")
        String status,
        @Size(max = 2000, message = "Status note must be 2,000 characters or fewer.")
        String note) {
}
