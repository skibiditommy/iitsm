package com.iitsm.domain.escalation;

import com.iitsm.domain.enums.EscalationLevel;
import com.iitsm.domain.model.AuditLogEntry;
import com.iitsm.domain.model.Incident;

/**
 * Concrete Handler 4: Terminal Tier (IT Manager) - Highest executive priority.
 */
public class ItManagerEscalationHandler extends BaseEscalationHandler {

    public ItManagerEscalationHandler() {
        super(EscalationLevel.IT_MANAGER);
    }

    @Override
    public boolean handleEscalation(Incident incident, String reason, String actorEmail) {
        if (incident.getEscalationLevel() == EscalationLevel.IT_MANAGER) {
            incident.addAuditEntry(new AuditLogEntry(
                    actorEmail, "EXECUTIVE_ESCALATION_NOTIFIED",
                    EscalationLevel.IT_MANAGER.name(), EscalationLevel.IT_MANAGER.name(),
                    "Incident is at highest escalation tier (IT Manager). Executive breach alert broadcast: " + reason
            ));
            return true;
        }
        return passToNext(incident, reason, actorEmail);
    }
}
