package com.jobtrack.jobtrack.controller;

import com.jobtrack.jobtrack.entity.Application;
import com.jobtrack.jobtrack.dto.JobDescriptionExtractionRequest;
import com.jobtrack.jobtrack.dto.JobDescriptionExtractionResponse;
import com.jobtrack.jobtrack.dto.StatusChangeRequest;
import com.jobtrack.jobtrack.dto.StatusHistoryResponse;
import com.jobtrack.jobtrack.service.JobDescriptionExtractionService;
import com.jobtrack.jobtrack.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    private final ApplicationService service;
    private final JobDescriptionExtractionService extractionService;

    public ApplicationController(ApplicationService service, JobDescriptionExtractionService extractionService) {
        this.service = service;
        this.extractionService = extractionService;
    }

    @PostMapping("/extract")
    public JobDescriptionExtractionResponse extractJobDescription(
            @Valid @RequestBody JobDescriptionExtractionRequest request) {
        return extractionService.extract(request.text());
    }

    @GetMapping
    public List<Application> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Application findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/{id}/status-history")
    public List<StatusHistoryResponse> findStatusHistory(@PathVariable Long id) {
        return service.findStatusHistory(id);
    }

    @PatchMapping("/{id}/status")
    public Application changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest request) {
        return service.changeStatus(id, request.status(), request.note());
    }

    @PostMapping
    public ResponseEntity<Application> create(@Valid @RequestBody Application application) {
        Application created = service.create(application);
        return ResponseEntity.created(URI.create("/api/applications/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public Application update(@PathVariable Long id, @Valid @RequestBody Application application) {
        return service.update(id, application);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
