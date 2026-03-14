package com.goservi.serviceoffer.controller;

import com.goservi.auth.repository.AuthUserRepository;
import com.goservi.common.exception.NotFoundException;
import com.goservi.serviceoffer.dto.ServiceOfferDtos;
import com.goservi.serviceoffer.repository.CategoryRepository;
import com.goservi.serviceoffer.service.ServiceOfferService;
import com.goservi.user.service.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
@Tag(name = "Services", description = "Anuncios de servicios de profesionales")
public class ServiceOfferController {

    private final ServiceOfferService service;
    private final CloudinaryService cloudinaryService;
    private final AuthUserRepository authRepo;
    private final CategoryRepository categoryRepo;
    private final com.goservi.serviceoffer.repository.SubcategoryRepository subcategoryRepo;

    @Operation(summary = "Crear anuncio de servicio")
    @PostMapping
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<ServiceOfferDtos.ServiceOfferResponse> create(
            @Valid @RequestBody ServiceOfferDtos.ServiceOfferRequest req,
            Principal principal) {
        Long userId = getAuthUserId(principal);
        return ResponseEntity.ok(service.create(userId, req));
    }

    @Operation(summary = "Actualizar mi anuncio")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<ServiceOfferDtos.ServiceOfferResponse> update(
            @PathVariable Long id,
            @RequestBody ServiceOfferDtos.ServiceOfferRequest req,
            Principal principal) {
        Long userId = getAuthUserId(principal);
        return ResponseEntity.ok(service.update(id, userId, req));
    }

    @Operation(summary = "Subir foto al anuncio")
    @PostMapping("/{id}/photos")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<ServiceOfferDtos.ServiceOfferResponse> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Principal principal) {
        Long userId = getAuthUserId(principal);
        String url = cloudinaryService.upload(file, "services/" + id);
        return ResponseEntity.ok(service.addPhoto(id, userId, url));
    }

    @Operation(summary = "Ver un anuncio por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ServiceOfferDtos.ServiceOfferResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @Operation(summary = "Servicios de un profesional (público)")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ServiceOfferDtos.ServiceOfferResponse>> byUser(
            @PathVariable Long userId) {
        return ResponseEntity.ok(service.getByUser(userId));
    }

    @Operation(summary = "Mis anuncios")
    @GetMapping("/my")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<List<ServiceOfferDtos.ServiceOfferResponse>> myOffers(Principal principal) {
        Long userId = getAuthUserId(principal);
        return ResponseEntity.ok(service.getByUser(userId));
    }

    @Operation(summary = "Buscar profesionales cercanos por GPS")
    @GetMapping("/nearby")
    public ResponseEntity<List<ServiceOfferDtos.ServiceOfferResponse>> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double radiusKm,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long subcategoryId) {
        return ResponseEntity.ok(service.searchNearby(lat, lng, radiusKm, categoryId, subcategoryId));
    }

    @Operation(summary = "Desactivar mi anuncio")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id, Principal principal) {
        Long userId = getAuthUserId(principal);
        service.deactivate(id, userId);
        return ResponseEntity.ok().build();
    }

    private Long getAuthUserId(Principal principal) {
        return authRepo.findByEmail(principal.getName())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"))
                .getId();
    }
}