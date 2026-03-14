package com.goservi.tracking.service;

import com.goservi.booking.repository.BookingRepository;
import com.goservi.common.exception.NotFoundException;
import com.goservi.tracking.dto.TrackingDtos;
import com.goservi.tracking.entity.TrackingLog;
import com.goservi.tracking.repository.TrackingLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TrackingService {

    private final TrackingLogRepository logRepo;
    private final BookingRepository bookingRepo;
    private final SimpMessagingTemplate ws;

    public TrackingDtos.TrackingResponse updateLocation(Long professionalId, TrackingDtos.LocationUpdate req) {
        var booking = bookingRepo.findById(req.getBookingId())
                .orElseThrow(() -> new NotFoundException("Reserva no encontrada"));

        if (!booking.getProfessionalId().equals(professionalId))
            throw new com.goservi.common.exception.UnauthorizedException("Sin permiso");

        // Calculate ETA using Haversine if client location is available
        Integer eta = null;
        Double distance = null;
        if (booking.getClientLat() != null && booking.getClientLng() != null) {
            distance = haversine(req.getLatitude(), req.getLongitude(),
                    booking.getClientLat(), booking.getClientLng());
            // Assume ~30 km/h average urban speed
            eta = (int) Math.ceil((distance / 30.0) * 60);
        }

        TrackingLog log = TrackingLog.builder()
                .bookingId(req.getBookingId())
                .professionalId(professionalId)
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .etaMinutes(eta)
                .build();
        logRepo.save(log);

        var response = TrackingDtos.TrackingResponse.builder()
                .bookingId(req.getBookingId())
                .professionalId(professionalId)
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .etaMinutes(eta)
                .distanceKm(distance)
                .build();

        // Broadcast to client
        ws.convertAndSend("/topic/tracking/" + req.getBookingId(), response);

        // Smart proximity notification
        if (eta != null && eta <= 5) {
            ws.convertAndSend("/topic/tracking/" + req.getBookingId() + "/nearby",
                    "El profesional está a menos de 5 minutos");
        }

        return response;
    }

    public TrackingDtos.TrackingResponse getLatest(String bookingId) {
        return logRepo.findTopByBookingIdOrderByRecordedAtDesc(bookingId)
                .map(t -> TrackingDtos.TrackingResponse.builder()
                        .bookingId(t.getBookingId())
                        .professionalId(t.getProfessionalId())
                        .latitude(t.getLatitude())
                        .longitude(t.getLongitude())
                        .etaMinutes(t.getEtaMinutes())
                        .build())
                .orElseThrow(() -> new NotFoundException("Sin datos de tracking para esta reserva"));
    }

    @Scheduled(cron = "0 0 3 * * *") // Daily at 3AM
    public void cleanOldLogs() {
        logRepo.deleteOlderThan(LocalDateTime.now().minusDays(7));
        log.info("Tracking logs cleanup done");
    }

    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
