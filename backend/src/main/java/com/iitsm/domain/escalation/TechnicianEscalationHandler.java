package com.iitsm.domain.escalation;

import com.iitsm.domain.enums.EscalationLevel;
import com.iitsm.domain.model.AuditLogEntry;
import com.iitsm.domain.model.Incident;

/**
 * Concrete Handler 1: Escalates from Tier 1 (Technician) to Tier 2 (Team Lead).
 */
public class TechnicianEscalationHandler extends BaseEscalationHandler {

    public TechnicianEscalationHandler() {
        super(EscalationLevel.TECHNICIAN);
    }

    @Override
    public boolean handleEscalation(Incident incident, String reason, String actorEmail) {
        if (incident.getEscalationLevel() == EscalationLevel.TECHNICIAN) {
            incident.escalate(EscalationLevel.TEAM_LEAD);
            incident.addAuditEntry(new AuditLogEntry(
                    actorEmail, "TIER_1_ESCALATED",
                    EscalationLevel.TECHNICIAN.name(), EscalationLevel.TEAM_LEAD.name(),
                    "Escalated from Technician to Team Lead: " + reason
            ));
            return true;
        }
        return passToNext(incident, reason, actorEmail);
    }
}
