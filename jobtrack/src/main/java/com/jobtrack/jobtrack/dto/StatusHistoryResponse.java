package com.jobtrack.jobtrack.dto;

import java.time.Instant;

public record StatusHistoryResponse(
        Long id,
        Long applicationId,
        String previousStatus,
        String status,
        Instant changedAt,
        String note) {
}
