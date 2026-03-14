package com.goservi.booking.entity;

public enum BookingStatus {
    PENDING,        // Esperando aceptación del profesional
    CONFIRMED,      // Profesional aceptó
    ARRIVED,      // Profesional llegó al destino — esperando código del cliente
    IN_PROGRESS,    // Código verificado, trabajo en curso
    COMPLETED,      // Trabajo finalizado, pendiente de pago
    PAID,           // Pago confirmado
    CANCELLED,      // Cancelado
    REJECTED        // Rechazado por el profesional
}
