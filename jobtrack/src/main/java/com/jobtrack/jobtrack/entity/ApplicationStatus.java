package com.jobtrack.jobtrack.entity;

import java.util.Arrays;

public enum ApplicationStatus {
    APPLIED("applied"),
    SHORTLISTED("shortlisted"),
    ONLINE_ASSESSMENT("online-assessment"),
    CODING_TEST("coding-test"),
    TECHNICAL_INTERVIEW("technical-interview"),
    HR_INTERVIEW("hr-interview"),
    OFFER("offer"),
    REJECTED("rejected"),
    WITHDRAWN("withdrawn");

    private final String value;

    ApplicationStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static boolean supports(String value) {
        return value != null && Arrays.stream(values()).anyMatch(status -> status.value.equals(value));
    }
}
