package com.goservi.payment.entity;

public enum WithdrawalStatus {
    PENDING,    // esperando que GoServi lo procese
    PROCESSING, // en curso
    PROCESSED,  // transferido
    REJECTED    // rechazado por algún motivo
}