package com.iitsm.domain.valueobject;

import jakarta.persistence.Embeddable;

import java.time.Duration;
import java.util.Objects;

@Embeddable
public class SlaTarget {

    private long responseMinutes;

    private long resolutionMinutes;

    /**
     * Protected no-arg constructor required by JPA.
     */
    protected SlaTarget() {
    }

    /**
     * Creates a new SlaTarget.
     *
     * @param responseMinutes   maximum response time in minutes (must be > 0)
     * @param resolutionMinutes maximum resolution time in minutes (must be > 0)
     * @throws IllegalArgumentException if either value is not positive
     */
    public SlaTarget(long responseMinutes, long resolutionMinutes) {
        if (responseMinutes <= 0) {
            throw new IllegalArgumentException(
                    "responseMinutes must be > 0, but was: " + responseMinutes);
        }
        if (resolutionMinutes <= 0) {
            throw new IllegalArgumentException(
                    "resolutionMinutes must be > 0, but was: " + resolutionMinutes);
        }
        this.responseMinutes = responseMinutes;
        this.resolutionMinutes = resolutionMinutes;
    }

    public long getResponseMinutes() {
        return responseMinutes;
    }

    public long getResolutionMinutes() {
        return resolutionMinutes;
    }

    /**
     * Returns the response target as a {@link Duration}.
     */
    public Duration getResponseDuration() {
        return Duration.ofMinutes(responseMinutes);
    }

    /**
     * Returns the resolution target as a {@link Duration}.
     */
    public Duration getResolutionDuration() {
        return Duration.ofMinutes(resolutionMinutes);
    }

    @Override
    public String toString() {
        return "SlaTarget{" +
                "responseMinutes=" + responseMinutes +
                ", resolutionMinutes=" + resolutionMinutes +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SlaTarget slaTarget = (SlaTarget) o;
        return responseMinutes == slaTarget.responseMinutes &&
                resolutionMinutes == slaTarget.resolutionMinutes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(responseMinutes, resolutionMinutes);
    }
}
