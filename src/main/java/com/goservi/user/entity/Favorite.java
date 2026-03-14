package com.goservi.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_favorites", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"client_id", "professional_id", "service_offer_id"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "professional_id", nullable = false)
    private Long professionalId;

    @Column(name = "service_offer_id")
    private Long serviceOfferId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}