package com.iitsm.controller;

import com.iitsm.dto.request.AdvanceTimeRequest;
import com.iitsm.scheduler.SlaMonitorScheduler;
import com.iitsm.service.DemoTimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoController {

    private final DemoTimeService demoTimeService;
    private final SlaMonitorScheduler slaMonitorScheduler;

    @PostMapping("/advance-time")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> advanceTime(@Valid @RequestBody AdvanceTimeRequest request) {
        long totalOffset = demoTimeService.advanceTime(request.minutes());
        return ResponseEntity.ok(Map.of(
                "advancedMinutes", request.minutes(),
                "totalOffsetMinutes", totalOffset,
                "demoTime", demoTimeService.now().toString()
        ));
    }

    @PostMapping("/trigger-sla-check")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> triggerSlaCheck() {
        slaMonitorScheduler.triggerCheck();
        return ResponseEntity.ok(Map.of(
                "message", "SLA check triggered",
                "demoTime", demoTimeService.now().toString()
        ));
    }

    @PostMapping("/reset-time")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> resetTime() {
        demoTimeService.resetTime();
        return ResponseEntity.ok(Map.of("message", "Demo time reset to real time"));
    }

    @GetMapping("/time")
    public ResponseEntity<Map<String, Object>> getTime() {
        return ResponseEntity.ok(Map.of(
                "demoTime", demoTimeService.now().toString(),
                "offsetMinutes", demoTimeService.getOffsetMinutes()
        ));
    }
}
