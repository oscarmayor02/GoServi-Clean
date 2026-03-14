package com.goservi.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Solicitud de retiro de un profesional.
 * Estado: PENDING → PROCESSED (admin lo marca) | REJECTED
 */
@Entity
@Table(name = "withdrawal_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WithdrawalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "professional_id", nullable = false)
    private Long professionalId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    /** Nequi, Bancolombia, Daviplata, etc. */
    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "account_holder")
    private String accountHolder;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private WithdrawalStatus status = WithdrawalStatus.PENDING;

    @Column(name = "admin_note")
    private String adminNote;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}