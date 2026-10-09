package com.jobtrack.jobtrack.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "application_status_history")
public class ApplicationStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 30)
    private String toStatus;

    @Convert(converter = InstantUtcDateTimeConverter.class)
    @Column(name = "changed_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant changedAt;

    @Column(columnDefinition = "TEXT")
    private String note;

    protected ApplicationStatusHistory() {
    }

    public ApplicationStatusHistory(Long applicationId, String fromStatus, String toStatus,
                                    Instant changedAt, String note) {
        this.applicationId = applicationId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedAt = changedAt;
        this.note = note;
    }

    @PrePersist
    void setServerTimestampIfMissing() {
        if (changedAt == null) changedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getApplicationId() { return applicationId; }
    public String getFromStatus() { return fromStatus; }
    public String getToStatus() { return toStatus; }
    public Instant getChangedAt() { return changedAt; }
    public String getNote() { return note; }
}
