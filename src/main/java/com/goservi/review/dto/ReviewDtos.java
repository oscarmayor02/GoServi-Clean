package com.goservi.review.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class ReviewDtos {

    @Data
    public static class ReviewRequest {
        @NotBlank private String bookingId;
        @NotNull @Min(1) @Max(5) private Integer rating;
        @Size(max = 1000) private String comment;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ReviewResponse {
        private Long id;
        private String bookingId;
        private Long reviewerId;
        private String reviewerName;
        private String reviewerPhoto;
        private Long reviewedId;
        private Integer rating;
        private String comment;
        private LocalDateTime createdAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RatingSummary {
        private Long userId;
        private Double averageRating;
        private Long reviewCount;
        private List<ReviewResponse> reviews;
    }
}
