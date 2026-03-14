package com.goservi.tracking.controller;

import com.goservi.auth.repository.AuthUserRepository;
import com.goservi.common.exception.NotFoundException;
import com.goservi.tracking.dto.TrackingDtos;
import com.goservi.tracking.service.TrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/tracking")
@RequiredArgsConstructor
@Tag(name = "Tracking", description = "Rastreo GPS del profesional en tiempo real")
public class TrackingController {

    private final TrackingService trackingService;
    private final AuthUserRepository authRepo;

    @Operation(summary = "Profesional actualiza su ubicación")
    @PostMapping("/update")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<TrackingDtos.TrackingResponse> update(
            @RequestBody TrackingDtos.LocationUpdate req,
            Principal principal) {
        Long userId = getAuthUserId(principal);
        return ResponseEntity.ok(trackingService.updateLocation(userId, req));
    }

    @Operation(summary = "Cliente obtiene última ubicación del profesional")
    @GetMapping("/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TrackingDtos.TrackingResponse> getLatest(@PathVariable String bookingId) {
        return ResponseEntity.ok(trackingService.getLatest(bookingId));
    }

    private Long getAuthUserId(Principal principal) {
        return authRepo.findByEmail(principal.getName())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"))
                .getId();
    }
}
