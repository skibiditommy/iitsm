package com.iitsm.controller;

import com.iitsm.dto.request.*;
import com.iitsm.dto.response.*;
import com.iitsm.infrastructure.adapter.AiAssistantPort;
import com.iitsm.service.IncidentService;
import com.iitsm.service.SearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;
    private final SearchService searchService;
    private final AiAssistantPort aiAssistant;

    @PostMapping
    public ResponseEntity<IncidentResponse> create(@Valid @RequestBody CreateIncidentRequest request,
                                                     Authentication auth) {
        return ResponseEntity.ok(incidentService.createIncident(request, auth.getName()));
    }

    @GetMapping
    public ResponseEntity<List<IncidentSummaryResponse>> getAll() {
        return ResponseEntity.ok(incidentService.getAllIncidents());
    }

    @GetMapping("/active")
    public ResponseEntity<List<IncidentSummaryResponse>> getActive() {
        return ResponseEntity.ok(incidentService.getActiveIncidents());
    }

    @GetMapping("/my")
    public ResponseEntity<List<IncidentSummaryResponse>> getMyIncidents(Authentication auth) {
        return ResponseEntity.ok(incidentService.getIncidentsByReporter(auth.getName()));
    }

    @GetMapping("/assigned")
    public ResponseEntity<List<IncidentSummaryResponse>> getAssigned(Authentication auth) {
        return ResponseEntity.ok(incidentService.getIncidentsByTechnician(auth.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(incidentService.getIncident(id));
    }

    @GetMapping("/by-display-id/{displayId}")
    public ResponseEntity<IncidentResponse> getByDisplayId(@PathVariable String displayId) {
        return ResponseEntity.ok(incidentService.getIncidentByDisplayId(displayId));
    }

    @PutMapping("/{id}/classify")
    public ResponseEntity<IncidentResponse> classify(@PathVariable Long id,
                                                       @Valid @RequestBody ClassifyIncidentRequest request,
                                                       Authentication auth) {
        return ResponseEntity.ok(incidentService.classifyIncident(id, request, auth.getName()));
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<IncidentResponse> assign(@PathVariable Long id,
                                                     @RequestBody(required = false) AssignTechnicianRequest request,
                                                     Authentication auth) {
        return ResponseEntity.ok(incidentService.assignTechnician(id, request, auth.getName()));
    }

    @PutMapping("/{id}/acknowledge")
    public ResponseEntity<IncidentResponse> acknowledge(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(incidentService.acknowledgeIncident(id, auth.getName()));
    }

    @PutMapping("/{id}/investigate")
    public ResponseEntity<IncidentResponse> investigate(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(incidentService.startInvestigation(id, auth.getName()));
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<IncidentResponse> resolve(@PathVariable Long id,
                                                      @Valid @RequestBody ResolveIncidentRequest request,
                                                      Authentication auth) {
        return ResponseEntity.ok(incidentService.resolveIncident(id, request, auth.getName()));
    }

    @PutMapping("/{id}/verify")
    public ResponseEntity<IncidentResponse> verify(@PathVariable Long id,
                                                     @Valid @RequestBody VerifyIncidentRequest request,
                                                     Authentication auth) {
        return ResponseEntity.ok(incidentService.verifyIncident(id, request, auth.getName()));
    }

    @PutMapping("/{id}/escalate")
    public ResponseEntity<IncidentResponse> escalate(@PathVariable Long id,
                                                       @RequestBody(required = false) EscalateRequest request,
                                                       Authentication auth) {
        return ResponseEntity.ok(incidentService.escalateIncident(id, request, auth.getName()));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<IncidentResponse> addComment(@PathVariable Long id,
                                                         @Valid @RequestBody AddCommentRequest request,
                                                         Authentication auth) {
        return ResponseEntity.ok(incidentService.addComment(id, request, auth.getName()));
    }

    @GetMapping("/{id}/suggestions")
    public ResponseEntity<List<SuggestedResolutionResponse>> getSuggestions(@PathVariable Long id) {
        return ResponseEntity.ok(incidentService.getSuggestedResolutions(id));
    }

    @GetMapping("/{id}/duplicates")
    public ResponseEntity<List<DuplicateCandidateResponse>> getDuplicates(@PathVariable Long id) {
        return ResponseEntity.ok(incidentService.detectDuplicates(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<IncidentSummaryResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String slaStatus,
            @RequestParam(required = false) Long technicianId,
            @RequestParam(required = false) Long reporterId,
            @RequestParam(required = false) Long serviceId) {
        return ResponseEntity.ok(searchService.search(keyword, status, priority, category,
                slaStatus, technicianId, reporterId, serviceId));
    }

    @PutMapping("/{id}/deadline")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IncidentResponse> updateDeadline(
            @PathVariable Long id,
            @RequestBody com.iitsm.dto.request.UpdateDeadlineRequest request,
            Authentication auth) {
        return ResponseEntity.ok(incidentService.updateDeadline(id, request, auth.getName()));
    }

    @PutMapping("/{id}/reopen")
    public ResponseEntity<IncidentResponse> reopen(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, String> body,
            Authentication auth) {
        String reason = body != null && body.containsKey("reason") ? body.get("reason") : "Resolution rejected by employee";
        return ResponseEntity.ok(incidentService.reopenIncident(id, reason, auth.getName()));
    }

    @PutMapping("/{id}/request-info")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    public ResponseEntity<IncidentResponse> requestInfo(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body,
            Authentication auth) {
        String question = body != null && body.containsKey("question") ? body.get("question") : "Please provide additional details";
        return ResponseEntity.ok(incidentService.requestIncidentInfo(id, question, auth.getName()));
    }

    @PutMapping("/{id}/resume")
    public ResponseEntity<IncidentResponse> resume(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, String> body,
            Authentication auth) {
        String reason = body != null && body.containsKey("reason") ? body.get("reason") : "Information provided, work resumed";
        return ResponseEntity.ok(incidentService.resumeIncident(id, reason, auth.getName()));
    }

    @PutMapping("/{id}/reassign")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    public ResponseEntity<IncidentResponse> reassign(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Long> body,
            Authentication auth) {
        Long techId = body != null ? body.get("technicianId") : null;
        if (techId == null) {
            throw new IllegalArgumentException("technicianId is required for reassignment");
        }
        return ResponseEntity.ok(incidentService.reassignIncident(id, techId, auth.getName()));
    }

    @PostMapping("/chatbot")
    public ResponseEntity<ChatbotResponse> chatbot(@Valid @RequestBody ChatbotMessageRequest request) {
        AiAssistantPort.ChatbotProcessingResult result = aiAssistant.processChat(
                request.message(), request.sessionId());
        return ResponseEntity.ok(new ChatbotResponse(result.reply(), result.suggestedActions(),
                request.sessionId() != null ? request.sessionId() : java.util.UUID.randomUUID().toString()));
    }
}
