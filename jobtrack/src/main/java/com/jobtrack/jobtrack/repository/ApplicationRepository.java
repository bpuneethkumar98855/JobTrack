package com.jobtrack.jobtrack.repository;

import com.jobtrack.jobtrack.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
}
