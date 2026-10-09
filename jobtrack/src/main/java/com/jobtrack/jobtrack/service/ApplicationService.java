package com.jobtrack.jobtrack.service;

import com.jobtrack.jobtrack.entity.Application;
import com.jobtrack.jobtrack.entity.ApplicationStatus;
import com.jobtrack.jobtrack.entity.ApplicationStatusHistory;
import com.jobtrack.jobtrack.dto.StatusHistoryResponse;
import com.jobtrack.jobtrack.repository.ApplicationRepository;
import com.jobtrack.jobtrack.repository.ApplicationStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class ApplicationService {
    private final ApplicationRepository repository;
    private final ApplicationStatusHistoryRepository historyRepository;

    public ApplicationService(ApplicationRepository repository, ApplicationStatusHistoryRepository historyRepository) {
        this.repository = repository;
        this.historyRepository = historyRepository;
    }

    @Transactional(readOnly = true)
    public List<Application> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Application findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    public Application create(Application application) {
        application.setId(null);
        application.setCreatedAt(null);
        application.setUpdatedAt(null);
        if (application.getStatus() == null || application.getStatus().isBlank()) {
            application.setStatus(ApplicationStatus.APPLIED.value());
        }
        validateStatus(application.getStatus());

        Application saved = repository.save(application);
        historyRepository.save(new ApplicationStatusHistory(
                saved.getId(), null, saved.getStatus(), Instant.now(), null));
        return saved;
    }

    public Application update(Long id, Application changes) {
        Application existing = findById(id);
        String previousStatus = existing.getStatus();
        String requestedStatus = changes.getStatus();
        validateStatus(requestedStatus);
        boolean statusChanged = !Objects.equals(previousStatus, requestedStatus);
        if (statusChanged) validateTransition(id, previousStatus, requestedStatus);

        existing.setCompany(changes.getCompany());
        existing.setRole(changes.getRole());
        existing.setApplicationDate(changes.getApplicationDate());
        existing.setStatus(requestedStatus);
        existing.setLocation(changes.getLocation());
        existing.setSource(changes.getSource());
        existing.setInterviewDate(changes.getInterviewDate());
        existing.setInterviewType(changes.getInterviewType());
        existing.setJobUrl(changes.getJobUrl());
        existing.setJobDescription(changes.getJobDescription());
        existing.setRequiredSkills(changes.getRequiredSkills());
        existing.setNotes(changes.getNotes());
        Application saved = repository.save(existing);
        if (statusChanged) recordStatusChange(id, previousStatus, requestedStatus, null);
        return saved;
    }

    public Application changeStatus(Long id, String newStatus, String note) {
        Application existing = findById(id);
        validateStatus(newStatus);
        String previousStatus = existing.getStatus();
        if (Objects.equals(previousStatus, newStatus)) return existing;
        validateTransition(id, previousStatus, newStatus);

        existing.setStatus(newStatus);
        Application saved = repository.save(existing);
        recordStatusChange(id, previousStatus, newStatus, note);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> findStatusHistory(Long applicationId) {
        findById(applicationId);
        return historyRepository.findByApplicationIdOrderByChangedAtAscIdAsc(applicationId).stream()
                .map(event -> new StatusHistoryResponse(
                        event.getId(), event.getApplicationId(), event.getFromStatus(),
                        event.getToStatus(), event.getChangedAt(), event.getNote()))
                .toList();
    }

    private void recordStatusChange(Long applicationId, String previousStatus, String newStatus, String note) {
        historyRepository.save(new ApplicationStatusHistory(
                applicationId, previousStatus, newStatus, Instant.now(), normalizeNote(note)));
    }

    private String normalizeNote(String note) {
        if (note == null || note.isBlank()) return null;
        return note.trim();
    }

    private void validateStatus(String status) {
        if (!ApplicationStatus.supports(status)) throw new InvalidApplicationStatusException(status);
    }

    private void validateTransition(Long applicationId, String previousStatus, String newStatus) {
        if (ApplicationStatus.REJECTED.value().equals(newStatus)
                && (ApplicationStatus.OFFER.value().equals(previousStatus)
                || historyRepository.existsByApplicationIdAndToStatus(
                        applicationId, ApplicationStatus.OFFER.value()))) {
            throw new InvalidStatusTransitionException();
        }
    }

    public void delete(Long id) {
        Application existing = findById(id);
        repository.delete(existing);
    }
}
