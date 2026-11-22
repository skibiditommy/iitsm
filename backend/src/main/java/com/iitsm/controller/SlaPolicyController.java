package com.iitsm.controller;

import com.iitsm.domain.model.SlaPolicy;
import com.iitsm.dto.request.UpdateSlaPolicyRequest;
import com.iitsm.dto.response.SlaPolicyResponse;
import com.iitsm.repository.SlaPolicyRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sla-policies")
@RequiredArgsConstructor
public class SlaPolicyController {

    private final SlaPolicyRepository slaPolicyRepository;

    @GetMapping
    public ResponseEntity<List<SlaPolicyResponse>> getAll() {
        return ResponseEntity.ok(slaPolicyRepository.findAll().stream()
                .map(this::toResponse).collect(Collectors.toList()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SlaPolicyResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateSlaPolicyRequest request) {
        SlaPolicy policy = slaPolicyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("SLA Policy not found: " + id));
        // SlaPolicy is immutable by design; create a replacement
        SlaPolicy updated = new SlaPolicy(policy.getPriority(), policy.getService(),
                request.responseMinutes(), request.resolutionMinutes());
        slaPolicyRepository.delete(policy);
        updated = slaPolicyRepository.save(updated);
        return ResponseEntity.ok(toResponse(updated));
    }

    private SlaPolicyResponse toResponse(SlaPolicy p) {
        return new SlaPolicyResponse(p.getId(), p.getPriority().name(),
                p.getService() != null ? p.getService().getId() : null,
                p.getService() != null ? p.getService().getName() : "Default",
                p.getResponseMinutes(), p.getResolutionMinutes());
    }
}
