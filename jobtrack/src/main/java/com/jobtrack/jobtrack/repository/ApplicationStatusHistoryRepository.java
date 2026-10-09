package com.jobtrack.jobtrack.repository;

import com.jobtrack.jobtrack.entity.ApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, Long> {
    List<ApplicationStatusHistory> findByApplicationIdOrderByChangedAtAscIdAsc(Long applicationId);
    boolean existsByApplicationIdAndToStatus(Long applicationId, String toStatus);
}
