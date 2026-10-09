package com.jobtrack.jobtrack.dto;

public record JobDescriptionExtractionResponse(
        String company,
        String role,
        String location,
        String jobDescription,
        String requiredSkills,
        String jobUrl) {
}
