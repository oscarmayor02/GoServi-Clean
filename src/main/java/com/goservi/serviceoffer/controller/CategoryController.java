package com.goservi.serviceoffer.controller;

import com.goservi.serviceoffer.dto.ServiceOfferDtos;
import com.goservi.serviceoffer.repository.CategoryRepository;
import com.goservi.serviceoffer.repository.SubcategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Categorías y subcategorías de servicios")
public class CategoryController {

    private final CategoryRepository categoryRepo;
    private final SubcategoryRepository subcategoryRepo;

    @Operation(summary = "Listar todas las categorías activas")
    @GetMapping("/all")
    public ResponseEntity<List<ServiceOfferDtos.CategoryResponse>> getAll() {
        return ResponseEntity.ok(categoryRepo.findByActiveTrue().stream()
                .map(c -> ServiceOfferDtos.CategoryResponse.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .description(c.getDescription())
                        .iconUrl(c.getIconUrl())
                        .build())
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Subcategorías por categoría")
    @GetMapping("/{categoryId}/subcategories")
    public ResponseEntity<List<ServiceOfferDtos.SubcategoryResponse>> getSubcategories(
            @PathVariable Long categoryId) {
        return ResponseEntity.ok(subcategoryRepo.findByCategoryIdAndActiveTrue(categoryId).stream()
                .map(s -> ServiceOfferDtos.SubcategoryResponse.builder()
                        .id(s.getId())
                        .categoryId(categoryId)
                        .name(s.getName())
                        .description(s.getDescription())
                        .iconUrl(s.getIconUrl())
                        .build())
                .collect(Collectors.toList()));
    }
}