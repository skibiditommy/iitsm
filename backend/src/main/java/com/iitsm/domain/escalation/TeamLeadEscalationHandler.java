package com.iitsm.domain.escalation;

import com.iitsm.domain.enums.EscalationLevel;
import com.iitsm.domain.model.AuditLogEntry;
import com.iitsm.domain.model.Incident;

/**
 * Concrete Handler 2: Escalates from Tier 2 (Team Lead) to Tier 3 (System Admin).
 */
public class TeamLeadEscalationHandler extends BaseEscalationHandler {

    public TeamLeadEscalationHandler() {
        super(EscalationLevel.TEAM_LEAD);
    }

    @Override
    public boolean handleEscalation(Incident incident, String reason, String actorEmail) {
        if (incident.getEscalationLevel() == EscalationLevel.TEAM_LEAD) {
            incident.escalate(EscalationLevel.SYSTEM_ADMIN);
            incident.addAuditEntry(new AuditLogEntry(
                    actorEmail, "TIER_2_ESCALATED",
                    EscalationLevel.TEAM_LEAD.name(), EscalationLevel.SYSTEM_ADMIN.name(),
                    "Escalated from Team Lead to System Admin: " + reason
            ));
            return true;
        }
        return passToNext(incident, reason, actorEmail);
    }
}
