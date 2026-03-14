package com.goservi.zone.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "zone_requests")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ZoneRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String city;

    private String department;
    private String country;
    private String message;

    @Builder.Default
    private boolean attended = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
