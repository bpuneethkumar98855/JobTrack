package com.jobtrack.jobtrack.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Stores an unambiguous UTC instant in MySQL DATETIME, which has no timezone. */
@Converter
public class InstantUtcDateTimeConverter implements AttributeConverter<Instant, LocalDateTime> {
    @Override
    public LocalDateTime convertToDatabaseColumn(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }

    @Override
    public Instant convertToEntityAttribute(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
