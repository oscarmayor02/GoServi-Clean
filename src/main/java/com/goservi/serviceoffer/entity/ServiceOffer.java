package com.goservi.serviceoffer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_offers")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ServiceOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id")
    private Subcategory subcategory;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private BigDecimal pricePerHour;

    @Column
    private Integer discountPercent;

    // Location for this service
    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column
    private String city;

    @ElementCollection
    @CollectionTable(name = "service_photos", joinColumns = @JoinColumn(name = "service_id"))
    @Column(name = "photo_url")
    @Builder.Default
    private List<String> photoUrls = new ArrayList<>();

    @Column
    private Integer experienceYears;

    @Builder.Default
    @Column(nullable = false)
    private boolean instantBookingEnabled = true;

    @Builder.Default
    @Column(nullable = false)
    private Integer leadTimeMin = 15;

    @Builder.Default
    @Column(nullable = false)
    private Integer minDurationMin = 60;

    @Builder.Default
    @Column(nullable = false)
    private Integer maxDurationMin = 480;

    @Builder.Default
    @Column(nullable = false)
    private Integer dailyCapacity = 3;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
