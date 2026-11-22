package com.iitsm.domain.escalation;

import com.iitsm.domain.model.Incident;
import org.springframework.stereotype.Component;

/**
 * GoF Chain of Responsibility: Client/Orchestrator.
 * Assembles and manages the 4-tier escalation hierarchy:
 * Technician → Team Lead → System Admin → IT Manager.
 */
@Component
public class EscalationChain {

    private final EscalationHandler chainHead;

    public EscalationChain() {
        // Construct the 4-tier handler chain
        EscalationHandler techHandler = new TechnicianEscalationHandler();
        EscalationHandler leadHandler = new TeamLeadEscalationHandler();
        EscalationHandler adminHandler = new AdminEscalationHandler();
        EscalationHandler managerHandler = new ItManagerEscalationHandler();

        techHandler.setNext(leadHandler);
        leadHandler.setNext(adminHandler);
        adminHandler.setNext(managerHandler);

        this.chainHead = techHandler;
    }

    public boolean escalate(Incident incident, String reason, String actorEmail) {
        return chainHead.handleEscalation(incident, reason, actorEmail);
    }
}
