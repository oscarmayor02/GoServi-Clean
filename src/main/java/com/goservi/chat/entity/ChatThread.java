package com.goservi.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_threads")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ChatThread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long clientId;

    @Column(nullable = false)
    private Long professionalId;

    @Column(name = "service_offer_id")
    private Long serviceOfferId;

    @Column(name = "booking_id")
    private String bookingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ThreadKind kind = ThreadKind.INQUIRY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ThreadStatus status = ThreadStatus.OPEN;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime openedAt;

    private LocalDateTime closedAt;
}
