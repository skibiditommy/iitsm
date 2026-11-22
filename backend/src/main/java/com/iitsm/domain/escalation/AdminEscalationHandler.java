package com.iitsm.domain.escalation;

import com.iitsm.domain.enums.EscalationLevel;
import com.iitsm.domain.model.AuditLogEntry;
import com.iitsm.domain.model.Incident;

/**
 * Concrete Handler 3: Escalates from Tier 3 (System Admin) to Tier 4 (IT Manager).
 */
public class AdminEscalationHandler extends BaseEscalationHandler {

    public AdminEscalationHandler() {
        super(EscalationLevel.SYSTEM_ADMIN);
    }

    @Override
    public boolean handleEscalation(Incident incident, String reason, String actorEmail) {
        if (incident.getEscalationLevel() == EscalationLevel.SYSTEM_ADMIN) {
            incident.escalate(EscalationLevel.IT_MANAGER);
            incident.addAuditEntry(new AuditLogEntry(
                    actorEmail, "TIER_3_ESCALATED",
                    EscalationLevel.SYSTEM_ADMIN.name(), EscalationLevel.IT_MANAGER.name(),
                    "Escalated from System Admin to IT Manager: " + reason
            ));
            return true;
        }
        return passToNext(incident, reason, actorEmail);
    }
}
