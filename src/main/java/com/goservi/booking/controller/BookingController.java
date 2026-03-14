package com.goservi.booking.controller;

import com.goservi.auth.repository.AuthUserRepository;
import com.goservi.booking.dto.BookingDtos;
import com.goservi.booking.service.BookingService;
import com.goservi.common.exception.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Gestión de reservas")
public class BookingController {

    private final BookingService bookingService;
    private final AuthUserRepository authRepo;

    @Operation(summary = "Crear reserva")
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<BookingDtos.BookingResponse> create(
            @Valid @RequestBody BookingDtos.BookingRequest req,
            Principal principal) {
        return ResponseEntity.ok(bookingService.create(getAuthUserId(principal), req));
    }

    @Operation(summary = "Ver reserva por ID")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingDtos.BookingResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(bookingService.getById(id));
    }

    @Operation(summary = "Mis reservas como cliente")
    @GetMapping("/my/client")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<BookingDtos.BookingResponse>> myBookingsAsClient(Principal principal) {
        return ResponseEntity.ok(bookingService.getByClient(getAuthUserId(principal)));
    }

    @Operation(summary = "Mis reservas como profesional")
    @GetMapping("/my/professional")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<List<BookingDtos.BookingResponse>> myBookingsAsProfessional(Principal principal) {
        return ResponseEntity.ok(bookingService.getByProfessional(getAuthUserId(principal)));
    }

    @Operation(summary = "Aceptar reserva")
    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<BookingDtos.BookingResponse> accept(
            @PathVariable String id, Principal principal) {
        return ResponseEntity.ok(bookingService.accept(id, getAuthUserId(principal)));
    }

    @Operation(summary = "Rechazar reserva")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<BookingDtos.BookingResponse> reject(
            @PathVariable String id, Principal principal) {
        return ResponseEntity.ok(bookingService.reject(id, getAuthUserId(principal)));
    }

    @Operation(summary = "Verificar código de seguridad (CONFIRMED → IN_PROGRESS)")
    @PostMapping("/{id}/verify")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<BookingDtos.BookingResponse> verify(
            @PathVariable String id,
            @Valid @RequestBody BookingDtos.VerifyRequest req,
            Principal principal) {
        return ResponseEntity.ok(bookingService.verify(id, req, getAuthUserId(principal)));
    }

    @Operation(summary = "Profesional notifica llegada al destino — notifica al cliente por WS")
    @PostMapping("/{id}/arrive")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<BookingDtos.BookingResponse> arrive(
            @PathVariable String id, Principal principal) {
        return ResponseEntity.ok(bookingService.arrive(id, getAuthUserId(principal)));
    }

    @Operation(summary = "Finalizar trabajo (IN_PROGRESS → COMPLETED)")
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<BookingDtos.BookingResponse> complete(
            @PathVariable String id, Principal principal) {
        return ResponseEntity.ok(bookingService.complete(id, getAuthUserId(principal)));
    }

    @Operation(summary = "Confirmar pago recibido (COMPLETED → PAID)")
    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<BookingDtos.BookingResponse> pay(
            @PathVariable String id, Principal principal) {
        return ResponseEntity.ok(bookingService.markPaid(id, getAuthUserId(principal)));
    }

    @Operation(summary = "Cancelar reserva")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingDtos.BookingResponse> cancel(
            @PathVariable String id, Principal principal) {
        return ResponseEntity.ok(bookingService.cancel(id, getAuthUserId(principal)));
    }

    private Long getAuthUserId(Principal principal) {
        return authRepo.findByEmail(principal.getName())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"))
                .getId();
    }
}