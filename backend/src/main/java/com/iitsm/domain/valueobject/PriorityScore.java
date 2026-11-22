package com.iitsm.domain.valueobject;

import com.iitsm.domain.enums.Priority;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.Objects;

@Embeddable
public class PriorityScore {

    private double score;

    private String explanation;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    /**
     * Protected no-arg constructor required by JPA.
     */
    protected PriorityScore() {
    }

    /**
     * Creates a new PriorityScore.
     *
     * @param score       the numeric score (must be >= 0)
     * @param explanation a human-readable explanation of how the score was computed
     * @param priority    the resulting priority level
     * @throws IllegalArgumentException if score is negative or if explanation/priority is null
     */
    public PriorityScore(double score, String explanation, Priority priority) {
        if (score < 0) {
            throw new IllegalArgumentException("Score must be >= 0, but was: " + score);
        }
        if (explanation == null) {
            throw new IllegalArgumentException("Explanation must not be null");
        }
        if (priority == null) {
            throw new IllegalArgumentException("Priority must not be null");
        }
        this.score = score;
        this.explanation = explanation;
        this.priority = priority;
    }

    /**
     * Convenience constructor that accepts a priority label string (e.g. "P1", "P2").
     */
    public PriorityScore(double score, String explanation, String priorityLabel) {
        this(score, explanation, Priority.valueOf(priorityLabel));
    }

    public double getScore() {
        return score;
    }

    public String getExplanation() {
        return explanation;
    }

    public Priority getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return "PriorityScore{" +
                "score=" + score +
                ", explanation='" + explanation + '\'' +
                ", priority=" + priority +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PriorityScore that = (PriorityScore) o;
        return Double.compare(that.score, score) == 0 &&
                Objects.equals(explanation, that.explanation) &&
                priority == that.priority;
    }

    @Override
    public int hashCode() {
        return Objects.hash(score, explanation, priority);
    }
}
