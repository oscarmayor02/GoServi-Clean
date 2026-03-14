package com.goservi.booking.dto;

import com.goservi.booking.entity.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BookingDtos {

    @Data
    public static class BookingRequest {
        @NotNull private Long serviceOfferId;
        @NotNull private Long professionalId;
        @NotNull private LocalDateTime startLocal;
        @NotNull private LocalDateTime endLocal;
        private Double clientLat;
        private Double clientLng;
        private String clientAddress;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BookingResponse {
        private String id;
        private Long serviceOfferId;
        private Long clientId;
        private Long professionalId;
        private LocalDateTime startLocal;
        private LocalDateTime endLocal;
        private BookingStatus status;
        private String verificationCode;
        private LocalDateTime verifiedAt;
        private Double clientLat;
        private Double clientLng;
        private String clientAddress;
        private LocalDateTime createdAt;

        // Enriched — nombres
        private String clientName;
        private String professionalName;
        private String serviceTitle;

        // Enriched — precio calculado
        // totalPrice = pricePerHour * (durationMinutes / 60)
        // Con descuento si el serviceOffer tiene discountPercent > 0
        private BigDecimal pricePerHour;
        private Integer durationMinutes;
        private BigDecimal totalPrice;       // lo que paga el cliente
        private BigDecimal platformFee;      // 15% GoServi
        private BigDecimal professionalAmount; // 85% profesional
        private String clientPhoto;
        private String professionalPhoto;
    }

    @Data
    public static class VerifyRequest {
        @NotNull private String code;
    }
}