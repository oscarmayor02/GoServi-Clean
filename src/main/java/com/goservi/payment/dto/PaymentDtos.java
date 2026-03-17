package com.goservi.payment.dto;

import com.goservi.payment.entity.PaymentMethod;
import com.goservi.payment.entity.PaymentStatus;
import com.goservi.payment.entity.WithdrawalStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PaymentDtos {

    @Data
    public static class CreatePaymentRequest {
        private String bookingId;
        private BigDecimal amount;
        private String paymentMethod; // "WOMPI" o "CASH"
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PaymentResponse {
        private String id;
        private String bookingId;
        private BigDecimal amount;
        private BigDecimal platformFee;
        private BigDecimal professionalAmount;
        private PaymentStatus status;
        private PaymentMethod paymentMethod; // NUEVO
        private String paymentLink;
        private LocalDateTime createdAt;
        private LocalDateTime paidAt;
    }

    // ── Earnings ─────────────────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EarningsResponse {
        private BigDecimal totalEarned;
        private BigDecimal availableBalance;
        private BigDecimal pendingWithdrawal;
        private BigDecimal totalWithdrawn;
        private List<PaymentSummary> recentPayments;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PaymentSummary {
        private String bookingId;
        private BigDecimal amount;
        private BigDecimal platformFee;
        private BigDecimal professionalAmount;
        private LocalDateTime paidAt;
    }

    // ── Withdrawals ──────────────────────────────────────────────
    @Data
    public static class WithdrawalRequest {
        private BigDecimal amount;
        private String bankName;
        private String accountNumber;
        private String accountHolder;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class WithdrawalResponse {
        private String id;
        private BigDecimal amount;
        private String bankName;
        private String accountNumber;
        private String accountHolder;
        private WithdrawalStatus status;
        private LocalDateTime createdAt;
    }

    // ── Historial de pagos del cliente ───────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PaymentHistoryItem {
        private String id;
        private String bookingId;
        private String professionalName;
        private BigDecimal amount;
        private BigDecimal platformFee;
        private BigDecimal professionalAmount;
        private String status;
        private String wompiTransactionId;
        private LocalDateTime createdAt;
        private LocalDateTime paidAt;
    }

    // ── Wompi Webhook ────────────────────────────────────────────
    @Data
    public static class WompiWebhookEvent {
        private String event;
        private WompiData data;

        @Data
        public static class WompiData {
            private WompiTransaction transaction;
        }

        @Data
        public static class WompiTransaction {
            private String id;
            private String reference;
            private String status;
            private Integer amountInCents;
        }
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CashPaymentAdminView {
        private String paymentId;
        private String bookingId;
        private Long clientId;
        private String clientName;
        private Long professionalId;
        private String professionalName;
        private String professionalEmail;
        private BigDecimal totalAmount;
        private BigDecimal platformFee;       // lo que debe transferir el profesional
        private BigDecimal professionalAmount;
        private String status;
        private LocalDateTime createdAt;
    }
}