package com.iitsm.controller;

import com.iitsm.dto.request.UpdateTechnicianRequest;
import com.iitsm.dto.response.TechnicianResponse;
import com.iitsm.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/technicians")
@RequiredArgsConstructor
public class TechnicianController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<TechnicianResponse>> getAll() {
        return ResponseEntity.ok(userService.getAllTechnicians());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TechnicianResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getTechnicianById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TechnicianResponse> update(@PathVariable Long id,
                                                       @RequestBody UpdateTechnicianRequest request) {
        return ResponseEntity.ok(userService.updateTechnician(id, request));
    }
}
