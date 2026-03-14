package com.goservi.support.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_tickets")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column
    private String bookingId;   // opcional, referencia a la reserva

    @Column(nullable = false)
    private String type;        // COMPLAINT, QUESTION, REFUND, OTHER

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false)
    private String status;      // OPEN, IN_REVIEW, RESOLVED, CLOSED

    @Column(length = 1000)
    private String adminResponse;

    @Column
    private Long adminId;       // quien respondió

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}