package com.iitsm.dto.response;

import java.util.List;

public record ServiceImpactResponse(Long id, String name, List<Ticket> activeTickets,
                                    List<Dependent> dependents, long affectedDependentCount) {
    public record Ticket(Long id, String incidentId, String title) {}
    public record Dependent(Long id, List<String> path, List<Ticket> activeTickets) {}
}
