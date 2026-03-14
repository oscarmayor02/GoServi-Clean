package com.goservi.availability.dto;

import com.goservi.availability.entity.DayOfWeek;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class AvailabilityDtos {

    @Data
    public static class ScheduleRequest {
        private DayOfWeek dayOfWeek;
        private String startTime; // "08:00"
        private String endTime;   // "18:00"
        private boolean active = true;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ScheduleResponse {
        private Long id;
        private Long serviceOfferId;
        private DayOfWeek dayOfWeek;
        private String startTime;
        private String endTime;
        private boolean active;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TimeSlotDTO {
        private LocalDateTime startLocal;
        private LocalDateTime endLocal;
        private boolean instantBook;
    }

    @Data
    public static class BulkScheduleRequest {
        private List<ScheduleRequest> schedules;
    }
}
