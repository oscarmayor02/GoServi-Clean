package com.goservi.user.controller;

import com.goservi.auth.repository.AuthUserRepository;
import com.goservi.common.dto.UserSummary;
import com.goservi.common.exception.NotFoundException;
import com.goservi.user.dto.UserDtos;
import com.goservi.user.service.CloudinaryService;
import com.goservi.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Perfiles de usuario")
public class UserProfileController {

    private final UserProfileService profileService;
    private final CloudinaryService cloudinaryService;
    private final AuthUserRepository authRepo;

    @Operation(summary = "Obtener mi perfil")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDtos.ProfileResponse> getMyProfile(Principal principal) {
        Long authUserId = getAuthUserId(principal);
        return ResponseEntity.ok(profileService.getOrCreateProfile(authUserId));
    }

    @Operation(summary = "Actualizar mi perfil")
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDtos.ProfileResponse> updateMyProfile(
            @RequestBody UserDtos.ProfileRequest req,
            Principal principal) {
        Long authUserId = getAuthUserId(principal);
        return ResponseEntity.ok(profileService.updateProfile(authUserId, req));
    }

    @Operation(summary = "Subir foto por ID")
    @PostMapping("/{authUserId}/photo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDtos.ProfileResponse> uploadPhotoById(
            @PathVariable Long authUserId,
            @RequestParam("file") MultipartFile file) {
        String url = cloudinaryService.upload(file, "profiles");
        return ResponseEntity.ok(profileService.updatePhoto(authUserId, url));
    }

    @Operation(summary = "Obtener perfil público por ID (alias /user)")
    @GetMapping("/{authUserId}/user")
    public ResponseEntity<UserDtos.ProfileResponse> getProfileAlias(@PathVariable Long authUserId) {
        return ResponseEntity.ok(profileService.getById(authUserId));
    }

    @Operation(summary = "Obtener perfil público por ID")
    @GetMapping("/{authUserId}")
    public ResponseEntity<UserDtos.ProfileResponse> getProfile(@PathVariable Long authUserId) {
        return ResponseEntity.ok(profileService.getById(authUserId));
    }

    @Operation(summary = "Buscar profesionales cercanos por GPS")
    @GetMapping("/nearby")
    public ResponseEntity<List<UserDtos.ProfileResponse>> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double radiusKm) {
        return ResponseEntity.ok(profileService.searchNearby(lat, lng, radiusKm));
    }

    @Operation(summary = "Obtener resumen de usuario (interno)")
    @GetMapping("/{authUserId}/summary")
    public ResponseEntity<UserSummary> getSummary(@PathVariable Long authUserId) {
        return ResponseEntity.ok(profileService.getSummary(authUserId));
    }

    @PostMapping("/{authUserId}/mark-first-ad")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markFirstAd(@PathVariable Long authUserId) {
        profileService.markFirstAd(authUserId);
        return ResponseEntity.ok().build();
    }

    /**
     * Marcar onboarding como visto.
     * PATCH /users/{authUserId}/onboarding?role=CLIENT|PROFESSIONAL
     */
    @Operation(summary = "Marcar onboarding como visto")
    @PatchMapping("/{authUserId}/onboarding")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markOnboardingSeen(
            @PathVariable Long authUserId,
            @RequestParam String role) {
        profileService.markOnboardingSeen(authUserId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Crear o completar perfil de usuario")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDtos.ProfileResponse> createProfile(
            @RequestBody UserDtos.ProfileRequest req,
            Principal principal) {
        Long authUserId = getAuthUserId(principal);
        return ResponseEntity.ok(profileService.updateProfile(authUserId, req));
    }

    private Long getAuthUserId(Principal principal) {
        return authRepo.findByEmail(principal.getName())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"))
                .getId();
    }
}