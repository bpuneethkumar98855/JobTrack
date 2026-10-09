package com.jobtrack.jobtrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JobDescriptionExtractionRequest(
        @NotBlank(message = "Paste a job description before extracting details.")
        @Size(max = 50000, message = "The pasted job description must be 50,000 characters or fewer.")
        String text) {
}
