package com.goservi.serviceoffer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

public class ServiceOfferDtos {

    @Data
    public static class ServiceOfferRequest {
        @NotNull private Long categoryId;
        private Long subcategoryId;
        @NotBlank private String title;
        private String description;
        @NotNull private BigDecimal pricePerHour;
        private Integer discountPercent;
        private Double latitude;
        private Double longitude;
        private String city;
        private List<String> photoUrls;
        private Integer experienceYears;
        private boolean instantBookingEnabled = true;
        private Integer leadTimeMin = 15;
        private Integer minDurationMin = 60;
        private Integer maxDurationMin = 480;
        private Integer dailyCapacity = 3;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ServiceOfferResponse {
        private Long id;
        private Long userId;
        private Long categoryId;
        private String categoryName;
        private Long subcategoryId;
        private String subcategoryName;
        private String title;
        private String description;
        private BigDecimal pricePerHour;
        private Integer discountPercent;
        private Double latitude;
        private Double longitude;
        private String city;
        private List<String> photoUrls;
        private Integer experienceYears;
        private boolean instantBookingEnabled;
        private Integer leadTimeMin;
        private Integer minDurationMin;
        private Integer maxDurationMin;
        private Integer dailyCapacity;
        private boolean active;
        private Double distanceKm;
        // Professional info
        private String professionalName;
        private String professionalPhoto;
        private Double rating;
        private Integer reviewCount;
        private Long completedServices;
    }

    @Data
    public static class CategoryRequest {
        @NotBlank private String name;
        private String description;
        private String iconUrl;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CategoryResponse {
        private Long id;
        private String name;
        private String description;
        private String iconUrl;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SubcategoryResponse {
        private Long id;
        private Long categoryId;
        private String name;
        private String description;
        private String iconUrl;
    }
}
