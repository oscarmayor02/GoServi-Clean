package com.goservi.admin.controller;

import com.goservi.admin.dto.AdminDtos;
import com.goservi.admin.service.AdminService;
import com.goservi.payment.dto.PaymentDtos;
import com.goservi.payment.entity.WithdrawalStatus;
import com.goservi.payment.repository.WithdrawalRequestRepository;
import com.goservi.payment.repository.ProfessionalEarningRepository;
import com.goservi.payment.service.PaymentService;
import com.goservi.support.entity.SupportTicket;
import com.goservi.support.repository.SupportTicketRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Panel de administración — solo ADMIN")
public class AdminController {

    private final AdminService adminService;
    private final WithdrawalRequestRepository withdrawalRepo;
    private final ProfessionalEarningRepository earningsRepo;
    private final SupportTicketRepository ticketRepo;
    private final PaymentService paymentService;

    // ── DASHBOARD ──────────────────────────────────────────────

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDtos.DashboardStats> dashboard() {
        return ResponseEntity.ok(adminService.getDashboard());
    }

    @GetMapping("/dashboard/revenue-chart")
    public ResponseEntity<List<AdminDtos.RevenuePoint>> revenueChart(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(adminService.getRevenueChart(days));
    }

    @GetMapping("/dashboard/top-professionals")
    public ResponseEntity<List<AdminDtos.TopProfessional>> topProfessionals(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(adminService.getTopProfessionals(limit));
    }

    // ── USUARIOS ───────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<List<AdminDtos.UserAdminView>> getAllUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(adminService.getAllUsers(role, active));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<AdminDtos.UserAdminView> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    @PostMapping("/users/{id}/toggle-active")
    public ResponseEntity<Void> toggleUserActive(@PathVariable Long id) {
        adminService.toggleUserActive(id);
        return ResponseEntity.ok().build();
    }

    // ── RESERVAS ───────────────────────────────────────────────

    @GetMapping("/bookings")
    public ResponseEntity<List<AdminDtos.BookingAdminView>> getAllBookings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(adminService.getAllBookings(status, from, to));
    }

    // ── PAGOS ──────────────────────────────────────────────────

    @GetMapping("/payments")
    public ResponseEntity<List<AdminDtos.PaymentAdminView>> getAllPayments(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(adminService.getAllPayments(status));
    }

    // ── RETIROS ────────────────────────────────────────────────

    @GetMapping("/withdrawals")
    public ResponseEntity<List<AdminDtos.WithdrawalAdminView>> getAllWithdrawals(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(adminService.getAllWithdrawals(status));
    }

    @GetMapping("/withdrawals/pending")
    public ResponseEntity<List<AdminDtos.WithdrawalAdminView>> getPendingWithdrawals() {
        return ResponseEntity.ok(adminService.getAllWithdrawals("PENDING"));
    }

    @PostMapping("/withdrawals/{id}/process")
    public ResponseEntity<?> processWithdrawal(@PathVariable String id) {
        adminService.processWithdrawal(id);
        return ResponseEntity.ok(Map.of("message", "Retiro procesado correctamente"));
    }

    @PostMapping("/withdrawals/{id}/reject")
    public ResponseEntity<?> rejectWithdrawal(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        adminService.rejectWithdrawal(id, body.getOrDefault("reason", "Sin motivo"));
        return ResponseEntity.ok(Map.of("message", "Retiro rechazado. Saldo devuelto al profesional."));
    }

    // ── SERVICIOS ──────────────────────────────────────────────

    @GetMapping("/service-offers")
    public ResponseEntity<List<AdminDtos.ServiceOfferAdminView>> getAllOffers() {
        return ResponseEntity.ok(adminService.getAllOffers());
    }

    @PostMapping("/service-offers/{id}/toggle-active")
    public ResponseEntity<Void> toggleOffer(@PathVariable Long id) {
        adminService.toggleOfferActive(id);
        return ResponseEntity.ok().build();
    }

    // ── TICKETS / QUEJAS ───────────────────────────────────────

    @GetMapping("/tickets")
    public ResponseEntity<List<AdminDtos.TicketAdminView>> getAllTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(adminService.getAllTickets(status, type));
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<AdminDtos.TicketAdminView> getTicket(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getTicketById(id));
    }

    @PostMapping("/tickets/{id}/respond")
    public ResponseEntity<?> respondTicket(
            @PathVariable Long id,
            @RequestBody AdminDtos.TicketRespondRequest req,
            @AuthenticationPrincipal UserDetails user) {
        adminService.respondTicket(id, req, user);
        return ResponseEntity.ok(Map.of("message", "Respuesta enviada"));
    }

    // ── REPORTES ───────────────────────────────────────────────

    @GetMapping("/reports/revenue")
    public ResponseEntity<AdminDtos.RevenueReport> revenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(adminService.getRevenueReport(from, to));
    }

    // ── PAGOS EN EFECTIVO ──────────────────────────────────────

    @Operation(summary = "Pagos en efectivo pendientes de verificación")
    @GetMapping("/payments/cash/pending")
    public ResponseEntity<List<PaymentDtos.CashPaymentAdminView>> pendingCashPayments() {
        return ResponseEntity.ok(paymentService.getPendingCashPayments());
    }

    @Operation(summary = "Aprobar pago en efectivo (comisión verificada)")
    @PostMapping("/payments/{paymentId}/approve-cash")
    public ResponseEntity<?> approveCash(@PathVariable String paymentId) {
        var result = paymentService.approveCashPayment(paymentId);
        return ResponseEntity.ok(Map.of(
                "message", "Pago aprobado. Profesional liberado.",
                "payment", result
        ));
    }

    @Operation(summary = "Rechazar pago en efectivo (comisión no verificada)")
    @PostMapping("/payments/{paymentId}/reject-cash")
    public ResponseEntity<?> rejectCash(
            @PathVariable String paymentId,
            @RequestBody Map<String, String> body) {
        var result = paymentService.rejectCashPayment(
                paymentId, body.getOrDefault("reason", "Transferencia no verificada"));
        return ResponseEntity.ok(Map.of("message", "Pago rechazado.", "payment", result));
    }

    // ══════════════════════════════════════════════════════════
    // NUEVOS ENDPOINTS — Rankings, Detalle, Estadísticas
    // ══════════════════════════════════════════════════════════

    @Operation(summary = "Ranking de profesionales — ordenado por servicios completados")
    @GetMapping("/professionals/ranking")
    public ResponseEntity<List<AdminDtos.ProfessionalRanking>> professionalRanking(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(adminService.getProfessionalRanking(limit));
    }

    @Operation(summary = "Detalle de un profesional — historial, clientes, gráficos por mes")
    @GetMapping("/professionals/{id}/detail")
    public ResponseEntity<AdminDtos.ProfessionalDetail> professionalDetail(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getProfessionalDetail(id));
    }

    @Operation(summary = "Ranking de clientes — ordenado por servicios contratados")
    @GetMapping("/clients/ranking")
    public ResponseEntity<List<AdminDtos.ClientRanking>> clientRanking(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(adminService.getClientRanking(limit));
    }

    @Operation(summary = "Estadísticas mensuales — servicios, ingresos, usuarios por mes")
    @GetMapping("/stats/monthly")
    public ResponseEntity<AdminDtos.MonthlyStats> monthlyStats(
            @RequestParam(defaultValue = "12") int months) {
        return ResponseEntity.ok(adminService.getMonthlyStats(months));
    }
}