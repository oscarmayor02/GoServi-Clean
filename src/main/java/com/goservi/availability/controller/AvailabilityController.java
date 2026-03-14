package com.goservi.availability.controller;

import com.goservi.auth.repository.AuthUserRepository;
import com.goservi.availability.dto.AvailabilityDtos;
import com.goservi.availability.service.AvailabilityService;
import com.goservi.common.exception.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/availability")
@RequiredArgsConstructor
@Tag(name = "Availability", description = "Horarios y slots disponibles")
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final AuthUserRepository authRepo;

    @Operation(summary = "Guardar horario semanal del profesional (Paso 2 del anuncio)")
    @PostMapping("/schedule/{serviceOfferId}")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<List<AvailabilityDtos.ScheduleResponse>> saveSchedule(
            @PathVariable Long serviceOfferId,
            @RequestBody AvailabilityDtos.BulkScheduleRequest req) {
        return ResponseEntity.ok(availabilityService.saveSchedules(serviceOfferId, req));
    }

    @Operation(summary = "Ver horario de un servicio")
    @GetMapping("/schedule/{serviceOfferId}")
    public ResponseEntity<List<AvailabilityDtos.ScheduleResponse>> getSchedule(
            @PathVariable Long serviceOfferId) {
        return ResponseEntity.ok(availabilityService.getSchedule(serviceOfferId));
    }

    @Operation(summary = "Obtener slots disponibles para un servicio")
    @GetMapping("/slots")
    public ResponseEntity<List<AvailabilityDtos.TimeSlotDTO>> getSlots(
            @RequestParam Long serviceOfferId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "60") int durationMin) {
        return ResponseEntity.ok(
                availabilityService.getSlots(serviceOfferId, startDate, endDate, durationMin));
    }
}
