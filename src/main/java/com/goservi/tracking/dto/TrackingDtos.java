package com.goservi.tracking.dto;

import lombok.*;

public class TrackingDtos {

    @Data
    public static class LocationUpdate {
        private String bookingId;
        private Double latitude;
        private Double longitude;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TrackingResponse {
        private String bookingId;
        private Long professionalId;
        private Double latitude;
        private Double longitude;
        private Integer etaMinutes;
        private Double distanceKm;
    }
}
