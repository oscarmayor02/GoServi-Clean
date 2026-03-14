package com.goservi.user.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

public class UserDtos {

    @Data
    public static class ProfileRequest {
        private String fullName;
        private String bio;
        private String phone;
        private Double latitude;
        private Double longitude;
        private String address;
        private String city;
        // Extended
        private String gender;
        private LocalDate birthDate;
        private String documentType;
        private String documentNumber;
        private String street;
        private String streetNumber;
        private String province;
        private String postalCode;
        private String country;
        private String department;
        private Boolean onboardingSeen;
    }

    @Data
    @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class ProfileResponse {
        private Long id;
        private Long authUserId;
        private String fullName;
        private String bio;
        private String photoUrl;
        private String phone;
        private Double latitude;
        private Double longitude;
        private String address;
        private String city;
        private String gender;
        private LocalDate birthDate;
        private String documentType;
        private String documentNumber;
        private String street;
        private String streetNumber;
        private String province;
        private String postalCode;
        private String country;
        private String department;
        private boolean phoneVerified;
        private boolean identityVerified;
        private boolean firstAdCreated;
        private boolean onboardingSeen;
        private Double distanceKm;
    }

    @Data
    @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class FavoriteResponse {
        private Long id;
        private Long professionalId;
        private String professionalName;
        private String professionalPhoto;
    }
}
