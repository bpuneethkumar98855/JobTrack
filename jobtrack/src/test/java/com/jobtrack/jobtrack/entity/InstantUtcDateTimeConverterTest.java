package com.jobtrack.jobtrack.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InstantUtcDateTimeConverterTest {

    @Test
    void persistsUtcWallClockValueAndRestoresTheSameInstant() {
        Instant original = Instant.parse("2026-10-09T09:45:12.123456Z");
        InstantUtcDateTimeConverter converter = new InstantUtcDateTimeConverter();

        LocalDateTime stored = converter.convertToDatabaseColumn(original);

        assertEquals(LocalDateTime.of(2026, 10, 9, 9, 45, 12, 123456000), stored);
        assertEquals(original, converter.convertToEntityAttribute(stored));
    }
}
