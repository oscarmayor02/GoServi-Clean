package com.goservi.payment.controller;

import com.goservi.auth.repository.AuthUserRepository;
import com.goservi.common.exception.NotFoundException;
import com.goservi.payment.dto.PaymentDtos;
import com.goservi.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Gestión de pagos con Wompi")
public class PaymentController {

    private final PaymentService paymentService;
    private final AuthUserRepository authRepo;

    @Operation(summary = "Iniciar pago para una reserva")
    @PostMapping("/initiate")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<PaymentDtos.PaymentResponse> initiate(
            @RequestBody PaymentDtos.CreatePaymentRequest req,
            Principal principal) {
        return ResponseEntity.ok(paymentService.initiate(getUserId(principal), req));
    }

    @Operation(summary = "Ver pago de una reserva")
    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentDtos.PaymentResponse> getByBooking(@PathVariable String bookingId) {
        return ResponseEntity.ok(paymentService.getByBooking(bookingId));
    }

    @Operation(summary = "Webhook de Wompi — sin auth")
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody PaymentDtos.WompiWebhookEvent event,
            @RequestHeader(value = "X-Event-Checksum", required = false) String signature) {
        paymentService.handleWompiWebhook(event, signature);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Saldo acumulado del profesional")
    @GetMapping("/my/earnings")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<PaymentDtos.EarningsResponse> myEarnings(Principal principal) {
        return ResponseEntity.ok(paymentService.getEarnings(getUserId(principal)));
    }

    @Operation(summary = "Solicitar retiro de ganancias")
    @PostMapping("/my/withdraw")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<PaymentDtos.WithdrawalResponse> requestWithdrawal(
            @RequestBody PaymentDtos.WithdrawalRequest req,
            Principal principal) {
        return ResponseEntity.ok(paymentService.requestWithdrawal(getUserId(principal), req));
    }

    @Operation(summary = "Historial de retiros del profesional")
    @GetMapping("/my/withdrawals")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<List<PaymentDtos.WithdrawalResponse>> myWithdrawals(Principal principal) {
        return ResponseEntity.ok(paymentService.getWithdrawals(getUserId(principal)));
    }

    @Operation(summary = "Historial de pagos del cliente")
    @GetMapping("/my/history")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<PaymentDtos.PaymentHistoryItem>> myHistory(Principal principal) {
        return ResponseEntity.ok(paymentService.getClientHistory(getUserId(principal)));
    }

    private Long getUserId(Principal principal) {
        return authRepo.findByEmail(principal.getName())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"))
                .getId();
    }

    @Operation(summary = "Admin marca un pago como pagado (efectivo)")
    @PostMapping("/{paymentId}/admin-mark-paid")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentDtos.PaymentResponse> adminMarkPaid(
            @PathVariable String paymentId) {
        return ResponseEntity.ok(paymentService.adminMarkPaid(paymentId));
    }
}