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
        Long id;
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
}