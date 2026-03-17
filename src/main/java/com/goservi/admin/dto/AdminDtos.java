package com.goservi.admin.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class AdminDtos {

    // ── DASHBOARD ──────────────────────────────────────────────

    @Data @Builder
    public static class DashboardStats {
        long totalUsers;
        long totalClients;
        long totalProfessionals;
        long newUsersLast30Days;

        long totalServiceOffers;
        long activeServiceOffers;

        long totalBookings;
        long pendingBookings;
        long confirmedBookings;
        long inProgressBookings;
        long completedBookings;
        long cancelledBookings;
        long paidBookings;

        BigDecimal totalRevenue;
        BigDecimal platformRevenue;
        BigDecimal revenueThisWeek;
        BigDecimal revenueThisMonth;

        long totalPayments;
        long pendingPayments;

        long pendingWithdrawals;
        BigDecimal pendingWithdrawalsAmount;
        private long pendingCashPayments;
        long openTickets;

        long totalReviews;
        Double averageRating;
    }

    @Data @Builder
    public static class RevenuePoint {
        String date;
        BigDecimal amount;
        long bookingCount;
    }

    @Data @Builder
    public static class TopProfessional {
        Long id;
        String name;
        String photo;
        long completedBookings;
        BigDecimal totalEarned;
        double avgRating;
    }

    @Data @Builder
    public static class RevenueReport {
        String period;
        BigDecimal totalRevenue;
        BigDecimal platformFees;
        long totalBookings;
        long totalPayments;
        List<RevenuePoint> dailyBreakdown;
    }

    // ── USUARIOS ───────────────────────────────────────────────

    @Data @Builder
    public static class UserAdminView {
        Long id;
        String name;
        String email;
        String phone;
        String role;
        String activeRole;
        List<String> roles;
        boolean active;
        LocalDateTime createdAt;
        String profilePhoto;
        String fullName;
        String city;
        Double rating;
        Long reviewCount;
        long totalBookings;
        BigDecimal totalSpent;
        BigDecimal totalEarned;
    }

    // ── RESERVAS ───────────────────────────────────────────────

    @Data @Builder
    public static class BookingAdminView {
        String id;
        Long clientId;
        String clientName;
        String clientPhoto;
        Long professionalId;
        String professionalName;
        String professionalPhoto;
        Long serviceOfferId;
        String serviceTitle;
        String status;
        LocalDateTime scheduledAt;
        LocalDateTime createdAt;
        Integer durationMinutes;
        BigDecimal pricePerHour;
        BigDecimal totalPrice;
        BigDecimal platformFee;
        BigDecimal professionalAmount;
        String paymentStatus;
    }

    // ── PAGOS ──────────────────────────────────────────────────

    @Data @Builder
    public static class PaymentAdminView {
        String id;
        String bookingId;
        Long clientId;
        String clientName;
        Long professionalId;
        String professionalName;
        BigDecimal amount;
        BigDecimal platformFee;
        BigDecimal professionalAmount;
        String status;
        String wompiTransactionId;
        LocalDateTime createdAt;
        LocalDateTime paidAt;
    }

    // ── RETIROS ────────────────────────────────────────────────

    @Data @Builder
    public static class WithdrawalAdminView {
        private String id;
        Long professionalId;
        String professionalName;
        String professionalEmail;
        BigDecimal amount;
        String bankAccount;
        String bankName;
        String status;
        String rejectionReason;
        LocalDateTime createdAt;
        LocalDateTime processedAt;
    }

    // ── SERVICIOS ──────────────────────────────────────────────

    @Data @Builder
    public static class ServiceOfferAdminView {
        Long id;
        Long userId;
        String professionalName;
        String title;
        String category;
        String categoryName;
        String city;
        BigDecimal pricePerHour;
        boolean active;
        LocalDateTime createdAt;
        long totalBookings;
        Double rating;
        Long reviewCount;
    }

    // ── TICKETS ────────────────────────────────────────────────

    @Data @Builder
    public static class TicketAdminView {
        Long id;
        String userName;
        String userEmail;
        String userRole;
        String bookingId;
        String type;
        String description;
        String status;
        String adminResponse;
        LocalDateTime createdAt;
        LocalDateTime updatedAt;
    }

    @Data
    public static class TicketCreateRequest {
        String type;
        String description;
        String bookingId;
    }

    @Data
    public static class TicketRespondRequest {
        String response;
        String status;
    }

    // ══════════════════════════════════════════════════════════
    // NUEVOS DTOs — Rankings, Estadísticas, Detalle
    // ══════════════════════════════════════════════════════════

    // ── RANKING DE PROFESIONALES ──────────────────────────────

    @Data @Builder
    public static class ProfessionalRanking {
        Long id;
        String name;
        String photo;
        String email;
        long completedBookings;
        long uniqueClients;
        BigDecimal totalEarned;
        BigDecimal platformFeeGenerated;
        Double avgRating;
        Long reviewCount;
        long activeOffers;
        LocalDateTime memberSince;
    }

    // ── DETALLE DE PROFESIONAL ────────────────────────────────

    @Data @Builder
    public static class ProfessionalDetail {
        Long id;
        String name;
        String photo;
        String email;
        String phone;
        String city;
        Double avgRating;
        Long reviewCount;
        long totalBookings;
        long completedBookings;
        long cancelledBookings;
        BigDecimal totalEarned;
        BigDecimal availableBalance;
        long activeOffers;
        LocalDateTime memberSince;
        // Servicios por mes (últimos 12 meses)
        List<MonthlyCount> servicesByMonth;
        // Ingresos por mes
        List<MonthlyAmount> earningsByMonth;
        // Clientes atendidos
        List<ClientSummary> clients;
    }

    @Data @Builder
    public static class ClientSummary {
        Long id;
        String name;
        String photo;
        long bookingCount;
        BigDecimal totalSpent;
        LocalDateTime lastBooking;
    }

    // ── RANKING DE CLIENTES ──────────────────────────────────

    @Data @Builder
    public static class ClientRanking {
        Long id;
        String name;
        String photo;
        String email;
        long totalBookings;
        long completedBookings;
        BigDecimal totalSpent;
        long uniqueProfessionals;
        Double avgRatingGiven;
        LocalDateTime memberSince;
        LocalDateTime lastBooking;
    }

    // ── ESTADÍSTICAS MENSUALES ────────────────────────────────

    @Data @Builder
    public static class MonthlyStats {
        // Resumen del mes actual vs anterior
        MonthComparison bookings;
        MonthComparison revenue;
        MonthComparison newUsers;
        MonthComparison platformFees;
        // Desglose mensual (últimos 12 meses)
        List<MonthlyCount> bookingsByMonth;
        List<MonthlyAmount> revenueByMonth;
        List<MonthlyAmount> feesByMonth;
        List<MonthlyCount> newUsersByMonth;
        // Por categoría
        List<CategoryCount> bookingsByCategory;
        // Por método de pago
        List<PaymentMethodCount> paymentMethods;
    }

    @Data @Builder
    public static class MonthComparison {
        String label;
        double current;
        double previous;
        double changePercent;
    }

    @Data @Builder
    public static class MonthlyCount {
        String month;     // "2026-01", "2026-02", etc.
        String label;     // "Ene 2026", "Feb 2026", etc.
        long count;
    }

    @Data @Builder
    public static class MonthlyAmount {
        String month;
        String label;
        BigDecimal amount;
    }

    @Data @Builder
    public static class CategoryCount {
        String category;
        long count;
        BigDecimal revenue;
    }

    @Data @Builder
    public static class PaymentMethodCount {
        String method;    // "WOMPI", "CASH"
        long count;
        BigDecimal amount;
    }
}