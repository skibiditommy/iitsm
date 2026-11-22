package com.iitsm.domain.enums;

public enum Priority {

    P1("Critical", 5, 30),
    P2("High", 15, 120),
    P3("Medium", 60, 480),
    P4("Low", 240, 1440);

    private final String label;
    private final int responseMinutes;
    private final int resolutionMinutes;

    Priority(String label, int responseMinutes, int resolutionMinutes) {
        this.label = label;
        this.responseMinutes = responseMinutes;
        this.resolutionMinutes = resolutionMinutes;
    }

    public String getLabel() {
        return label;
    }

    public int getResponseMinutes() {
        return responseMinutes;
    }

    public int getResolutionMinutes() {
        return resolutionMinutes;
    }
}
