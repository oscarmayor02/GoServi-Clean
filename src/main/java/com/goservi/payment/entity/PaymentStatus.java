package com.goservi.payment.entity;

public enum PaymentStatus {
    PENDING,        // Wompi: esperando pago online
    PENDING_CASH,   // Efectivo: esperando que admin verifique comisión
    PAID,
    FAILED,
    REFUNDED
}