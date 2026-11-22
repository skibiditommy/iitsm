package com.iitsm.domain.enums;

public enum Urgency {

    CRITICAL(4, "Critical"),
    HIGH(3, "High"),
    MEDIUM(2, "Medium"),
    LOW(1, "Low");

    private final int value;
    private final String label;

    Urgency(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}
