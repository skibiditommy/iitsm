package com.iitsm.controller;

import com.iitsm.dto.response.AuditLogResponse;
import com.iitsm.repository.AuditLogEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogEntryRepository auditLogEntryRepository;

    @GetMapping("/incident/{incidentId}")
    public ResponseEntity<List<AuditLogResponse>> getByIncident(@PathVariable Long incidentId) {
        return ResponseEntity.ok(auditLogEntryRepository.findByIncidentId(incidentId).stream()
                .map(a -> new AuditLogResponse(a.getId(), a.getTimestamp(), a.getActorEmail(),
                        a.getAction(), a.getPreviousState(), a.getNewState(), a.getDescription()))
                .collect(Collectors.toList()));
    }

    @GetMapping
    public ResponseEntity<List<AuditLogResponse>> getAll() {
        return ResponseEntity.ok(auditLogEntryRepository.findAll().stream()
                .map(a -> new AuditLogResponse(a.getId(), a.getTimestamp(), a.getActorEmail(),
                        a.getAction(), a.getPreviousState(), a.getNewState(), a.getDescription()))
                .collect(Collectors.toList()));
    }
}
