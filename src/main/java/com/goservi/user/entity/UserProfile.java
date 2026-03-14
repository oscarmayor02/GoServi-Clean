package com.goservi.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_profiles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auth_user_id", nullable = false, unique = true)
    private Long authUserId;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "bio", length = 1000)
    private String bio;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "phone")
    private String phone;

    // Location (GPS coordinates)
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    // Extended personal info
    @Column(name = "gender")
    private String gender; // MALE, FEMALE, OTHER

    @Column(name = "birth_date")
    private java.time.LocalDate birthDate;

    @Column(name = "document_type")
    private String documentType; // CC, CE, PASSPORT, NIT

    @Column(name = "document_number")
    private String documentNumber;

    // Address
    @Column(name = "street")
    private String street;

    @Column(name = "street_number")
    private String streetNumber;

    @Column(name = "province")
    private String province;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "country")
    @Builder.Default
    private String country = "Colombia";

    @Column(name = "department")
    private String department;

    @Column(name = "first_ad_created")
    @Builder.Default
    private boolean firstAdCreated = false;

    @Column(name = "onboarding_seen")
    @Builder.Default
    private boolean onboardingSeen = false;

    @Column(name = "phone_verified")
    @Builder.Default
    private boolean phoneVerified = false;

    @Column(name = "identity_verified")
    @Builder.Default
    private boolean identityVerified = false;

    @ElementCollection
    @CollectionTable(name = "user_documents", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "doc_url")
    @Builder.Default
    private List<String> documentUrls = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
