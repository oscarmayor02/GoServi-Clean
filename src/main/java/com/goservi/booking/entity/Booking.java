package com.goservi.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private Long serviceOfferId;

    @Column(nullable = false)
    private Long clientId;

    @Column(nullable = false)
    private Long professionalId;

    @Column(nullable = false)
    private LocalDateTime startLocal;

    @Column(nullable = false)
    private LocalDateTime endLocal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(nullable = false, length = 6)
    private String verificationCode;

    private LocalDateTime verifiedAt;

    @Column(name = "client_lat")
    private Double clientLat;

    @Column(name = "client_lng")
    private Double clientLng;

    @Column(name = "client_address")
    private String clientAddress;

    private LocalDateTime cancelledAt;
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
