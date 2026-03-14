package com.goservi.tracking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tracking_logs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TrackingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String bookingId;

    @Column(nullable = false)
    private Long professionalId;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column
    private Integer etaMinutes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime recordedAt;
}
