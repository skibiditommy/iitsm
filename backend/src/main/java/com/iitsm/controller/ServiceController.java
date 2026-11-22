package com.iitsm.controller;

import com.iitsm.domain.model.ITService;
import com.iitsm.dto.request.CreateServiceRequest;
import com.iitsm.dto.response.ServiceResponse;
import com.iitsm.dto.response.ServiceSummary;
import com.iitsm.repository.ITServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ITServiceRepository serviceRepository;

    @GetMapping
    public ResponseEntity<List<ServiceResponse>> getAll() {
        return ResponseEntity.ok(serviceRepository.findAll().stream()
                .map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceResponse> getById(@PathVariable Long id) {
        ITService service = serviceRepository.findById(id)
                .orElseThrow(() -> new com.iitsm.domain.exception.ServiceNotFoundException(id));
        return ResponseEntity.ok(toResponse(service));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceResponse> create(@Valid @RequestBody CreateServiceRequest request) {
        ITService service = new ITService(request.name(), request.description(), request.businessCriticality());
        if (request.upstreamDependencyIds() != null) {
            for (Long depId : request.upstreamDependencyIds()) {
                serviceRepository.findById(depId).ifPresent(service::addUpstreamDependency);
            }
        }
        service = serviceRepository.save(service);
        return ResponseEntity.ok(toResponse(service));
    }

    private ServiceResponse toResponse(ITService s) {
        List<ServiceSummary> upstream = s.getUpstreamDependencies().stream()
                .map(d -> new ServiceSummary(d.getId(), d.getName(), d.getBusinessCriticality()))
                .collect(Collectors.toList());
        List<ServiceSummary> downstream = s.getDownstreamDependencies().stream()
                .map(d -> new ServiceSummary(d.getId(), d.getName(), d.getBusinessCriticality()))
                .collect(Collectors.toList());
        return new ServiceResponse(s.getId(), s.getName(), s.getDescription(),
                s.getBusinessCriticality(), upstream, downstream, s.getBlastRadius().size());
    }
}
