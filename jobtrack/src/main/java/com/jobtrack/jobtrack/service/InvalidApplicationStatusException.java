package com.jobtrack.jobtrack.service;

public class InvalidApplicationStatusException extends RuntimeException {
    public InvalidApplicationStatusException(String status) {
        super("Unsupported application status: " + status);
    }
}
