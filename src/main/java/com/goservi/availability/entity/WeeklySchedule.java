package com.goservi.availability.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "weekly_schedules", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"service_offer_id", "day_of_week"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class WeeklySchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_offer_id", nullable = false)
    private Long serviceOfferId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
