package com.jobtrack.jobtrack.service;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException() {
        super("This application cannot be marked Rejected because an Offer has already been recorded. Use Withdrawn if the candidate declines or leaves the process.");
    }
}
