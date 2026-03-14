package com.goservi.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Saldo acumulado de un profesional.
 * Se crea automáticamente al primer pago aprobado.
 * available_balance = total ganado - retirado - en_proceso
 */
@Entity
@Table(name = "professional_earnings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProfessionalEarning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "professional_id", nullable = false, unique = true)
    private Long professionalId;

    /** Total histórico acumulado (nunca baja) */
    @Builder.Default
    @Column(name = "total_earned", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalEarned = BigDecimal.ZERO;

    /** Saldo disponible para retirar */
    @Builder.Default
    @Column(name = "available_balance", nullable = false, precision = 14, scale = 2)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    /** Monto en retiros pendientes de procesar */
    @Builder.Default
    @Column(name = "pending_withdrawal", nullable = false, precision = 14, scale = 2)
    private BigDecimal pendingWithdrawal = BigDecimal.ZERO;

    /** Total ya retirado históricamente */
    @Builder.Default
    @Column(name = "total_withdrawn", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalWithdrawn = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}