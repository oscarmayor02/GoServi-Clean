package com.goservi.zone.controller;

import com.goservi.common.dto.NotificationRequest;
import com.goservi.common.dto.NotificationType;
import com.goservi.notification.service.NotificationService;
import com.goservi.zone.entity.ZoneRequest;
import com.goservi.zone.repository.ZoneRequestRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/zones")
@RequiredArgsConstructor
@Tag(name = "Zones", description = "Solicitudes de apertura en nuevas ciudades")
public class ZoneController {

    private final ZoneRequestRepository repo;
    private final NotificationService notificationService;

    @Value("${spring.mail.username:}")
    private String adminEmail;

    @Data
    public static class ZoneRequestDto {
        @Email @NotBlank private String email;
        @NotBlank private String city;
        private String department;
        private String country;
        private String message;
    }

    @Operation(summary = "Solicitar apertura en una nueva ciudad")
    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> request(@Valid @RequestBody ZoneRequestDto req) {
        ZoneRequest entity = ZoneRequest.builder()
                .email(req.getEmail())
                .city(req.getCity())
                .department(req.getDepartment())
                .country(req.getCountry() != null ? req.getCountry() : "Colombia")
                .message(req.getMessage())
                .build();
        repo.save(entity);

        // Notificar al admin
        try {
            notificationService.send(NotificationRequest.builder()
                    .to(adminEmail)
                    .type(NotificationType.EMAIL)
                    .subject("Nueva solicitud de zona: " + req.getCity())
                    .message("El usuario " + req.getEmail() + " solicita apertura en " +
                            req.getCity() + ", " + req.getDepartment() + ".\n\n" +
                            "Mensaje: " + (req.getMessage() != null ? req.getMessage() : "Sin mensaje"))
                    .build());

            // Confirmar al solicitante
            notificationService.send(NotificationRequest.builder()
                    .to(req.getEmail())
                    .type(NotificationType.EMAIL)
                    .subject("GoServi — Recibimos tu solicitud")
                    .message("Hola, recibimos tu solicitud para que GoServi llegue a " + req.getCity() +
                            ". Te avisaremos cuando esté disponible en tu zona.")
                    .build());
        } catch (Exception e) {
            log.warn("Could not send zone request notification: {}", e.getMessage());
        }

        return ResponseEntity.ok(Map.of("message",
                "Solicitud recibida. Te avisaremos cuando GoServi llegue a " + req.getCity()));
    }

    @Operation(summary = "Ver solicitudes de zona pendientes (solo ADMIN)")
    @GetMapping("/requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ZoneRequest>> getPending() {
        return ResponseEntity.ok(repo.findByAttendedFalse());
    }

    @Operation(summary = "Marcar solicitud como atendida (solo ADMIN)")
    @PostMapping("/requests/{id}/attend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> attend(@PathVariable Long id) {
        repo.findById(id).ifPresent(r -> {
            r.setAttended(true);
            repo.save(r);
        });
        return ResponseEntity.ok().build();
    }
}
