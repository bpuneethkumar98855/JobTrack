package com.jobtrack.jobtrack.service;

import com.jobtrack.jobtrack.dto.StatusHistoryResponse;
import com.jobtrack.jobtrack.entity.Application;
import com.jobtrack.jobtrack.entity.ApplicationStatusHistory;
import com.jobtrack.jobtrack.repository.ApplicationRepository;
import com.jobtrack.jobtrack.repository.ApplicationStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationStatusTimelineServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private ApplicationStatusHistoryRepository historyRepository;

    private ApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ApplicationService(applicationRepository, historyRepository);
    }

    @Test
    void newApplicationDefaultsToAppliedAndWritesInitialHistory() {
        Application application = validApplication();
        application.setStatus(null);
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> {
            Application saved = invocation.getArgument(0);
            saved.setId(41L);
            return saved;
        });

        Application created = service.create(application);

        assertEquals("applied", created.getStatus());
        ArgumentCaptor<ApplicationStatusHistory> event = ArgumentCaptor.forClass(ApplicationStatusHistory.class);
        verify(historyRepository).save(event.capture());
        assertEquals(41L, event.getValue().getApplicationId());
        assertNull(event.getValue().getFromStatus());
        assertEquals("applied", event.getValue().getToStatus());
        assertNull(event.getValue().getNote());
        assertNotNull(event.getValue().getChangedAt());
    }

    @Test
    void changedStatusWritesHistoryWithPreviousStatusTimestampAndNote() {
        Application existing = existingApplication("applied");
        when(applicationRepository.findById(41L)).thenReturn(Optional.of(existing));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.changeStatus(41L, "technical-interview", "  Interview scheduled.  ");

        ArgumentCaptor<ApplicationStatusHistory> event = ArgumentCaptor.forClass(ApplicationStatusHistory.class);
        verify(historyRepository).save(event.capture());
        assertEquals("applied", event.getValue().getFromStatus());
        assertEquals("technical-interview", event.getValue().getToStatus());
        assertEquals("Interview scheduled.", event.getValue().getNote());
        assertNotNull(event.getValue().getChangedAt());
        assertEquals("technical-interview", existing.getStatus());
    }

    @Test
    void unchangedStatusDoesNotCreateHistoryEvent() {
        Application existing = existingApplication("applied");
        when(applicationRepository.findById(41L)).thenReturn(Optional.of(existing));

        service.changeStatus(41L, "applied", "No change");

        verify(historyRepository, never()).save(any(ApplicationStatusHistory.class));
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void unrelatedEditsDoNotCreateHistoryEvent() {
        Application existing = existingApplication("shortlisted");
        Application changes = validApplication();
        changes.setCompany("Updated Company");
        changes.setStatus("shortlisted");
        when(applicationRepository.findById(41L)).thenReturn(Optional.of(existing));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(41L, changes);

        assertEquals("Updated Company", existing.getCompany());
        verify(historyRepository, never()).save(any(ApplicationStatusHistory.class));
    }

    @Test
    void putUpdateRecordsHistoryOnlyWhenStatusChanges() {
        Application existing = existingApplication("applied");
        Application changes = validApplication();
        changes.setStatus("online-assessment");
        when(applicationRepository.findById(41L)).thenReturn(Optional.of(existing));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(41L, changes);

        ArgumentCaptor<ApplicationStatusHistory> event = ArgumentCaptor.forClass(ApplicationStatusHistory.class);
        verify(historyRepository).save(event.capture());
        assertEquals("applied", event.getValue().getFromStatus());
        assertEquals("online-assessment", event.getValue().getToStatus());
        assertNull(event.getValue().getNote());
    }

    @Test
    void invalidStatusesAreRejectedOnCreatePutAndManualUpdate() {
        Application invalidCreate = validApplication();
        invalidCreate.setStatus("future-stage");
        assertThrows(InvalidApplicationStatusException.class, () -> service.create(invalidCreate));

        Application existing = existingApplication("applied");
        when(applicationRepository.findById(41L)).thenReturn(Optional.of(existing));
        Application invalidUpdate = validApplication();
        invalidUpdate.setStatus("future-stage");
        assertThrows(InvalidApplicationStatusException.class, () -> service.update(41L, invalidUpdate));
        assertThrows(InvalidApplicationStatusException.class,
                () -> service.changeStatus(41L, "future-stage", null));

        verifyNoInteractions(historyRepository);
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void rejectedIsBlockedWhenCurrentStatusIsOffer() {
        Application existing = existingApplication("offer");
        when(applicationRepository.findById(41L)).thenReturn(Optional.of(existing));

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.changeStatus(41L, "rejected", null));

        verify(applicationRepository, never()).save(any(Application.class));
        verify(historyRepository, never()).save(any(ApplicationStatusHistory.class));
    }

    @Test
    void rejectedIsBlockedThroughPutAfterAnOfferWasPreviouslyRecorded() {
        Application existing = existingApplication("shortlisted");
        Application changes = validApplication();
        changes.setStatus("rejected");
        when(applicationRepository.findById(41L)).thenReturn(Optional.of(existing));
        when(historyRepository.existsByApplicationIdAndToStatus(41L, "offer")).thenReturn(true);

        assertThrows(InvalidStatusTransitionException.class, () -> service.update(41L, changes));

        verify(applicationRepository, never()).save(any(Application.class));
        verify(historyRepository, never()).save(any(ApplicationStatusHistory.class));
    }

    @Test
    void missingApplicationIsReportedForManualUpdateAndHistoryRead() {
        when(applicationRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ApplicationNotFoundException.class,
                () -> service.changeStatus(404L, "applied", null));
        assertThrows(ApplicationNotFoundException.class,
                () -> service.findStatusHistory(404L));
        verifyNoInteractions(historyRepository);
    }

    @Test
    void historyReadKeepsRepositoryChronologicalOrder() {
        Application existing = existingApplication("technical-interview");
        when(applicationRepository.findById(41L)).thenReturn(Optional.of(existing));
        when(historyRepository.findByApplicationIdOrderByChangedAtAscIdAsc(41L)).thenReturn(List.of(
                event(41L, null, "applied", "2026-10-01T09:00:00Z"),
                event(41L, "applied", "shortlisted", "2026-10-02T09:00:00Z"),
                event(41L, "shortlisted", "technical-interview", "2026-10-03T09:00:00Z")));

        List<StatusHistoryResponse> result = service.findStatusHistory(41L);

        assertEquals(List.of("applied", "shortlisted", "technical-interview"),
                result.stream().map(StatusHistoryResponse::status).toList());
        verify(historyRepository).findByApplicationIdOrderByChangedAtAscIdAsc(41L);
    }

    @Test
    void createAndStatusUpdatesRunInWriteTransactions() throws Exception {
        assertNotNull(ApplicationService.class.getAnnotation(Transactional.class));
        org.junit.jupiter.api.Assertions.assertFalse(
                ApplicationService.class.getAnnotation(Transactional.class).readOnly());
    }

    private Application validApplication() {
        Application application = new Application();
        application.setCompany("Example Company");
        application.setRole("Developer");
        application.setApplicationDate(LocalDate.of(2026, 10, 9));
        application.setStatus("applied");
        return application;
    }

    private Application existingApplication(String status) {
        Application application = validApplication();
        application.setId(41L);
        application.setStatus(status);
        return application;
    }

    private ApplicationStatusHistory event(Long applicationId, String previous, String status, String timestamp) {
        return new ApplicationStatusHistory(applicationId, previous, status, Instant.parse(timestamp), null);
    }
}
