package com.iitsm.domain.enums;

public enum EscalationLevel {

    TECHNICIAN(1),
    TEAM_LEAD(2),
    SYSTEM_ADMIN(3),
    IT_MANAGER(4);

    private final int level;

    EscalationLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Returns the next escalation level in the hierarchy.
     *
     * @return the next {@link EscalationLevel}
     * @throws IllegalStateException if already at the highest level (IT_MANAGER)
     */
    public EscalationLevel next() {
        EscalationLevel[] levels = values();
        int nextOrdinal = this.ordinal() + 1;
        if (nextOrdinal >= levels.length) {
            throw new IllegalStateException(
                    "Cannot escalate beyond " + this.name() + " (level " + this.level + ")");
        }
        return levels[nextOrdinal];
    }
}
