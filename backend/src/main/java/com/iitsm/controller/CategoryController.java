package com.iitsm.controller;

import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.dto.request.CreateCategoryRequest;
import com.iitsm.dto.response.CategoryResponse;
import com.iitsm.repository.IncidentCategoryRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final IncidentCategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAll() {
        return ResponseEntity.ok(categoryRepository.findAll().stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getDescription()))
                .collect(Collectors.toList()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        IncidentCategory category = new IncidentCategory(request.name(), request.description());
        category = categoryRepository.save(category);
        return ResponseEntity.ok(new CategoryResponse(category.getId(), category.getName(), category.getDescription()));
    }
}
