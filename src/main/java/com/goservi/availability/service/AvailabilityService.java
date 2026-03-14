package com.goservi.availability.service;

import com.goservi.availability.dto.AvailabilityDtos;
import com.goservi.availability.entity.DayOfWeek;
import com.goservi.availability.entity.WeeklySchedule;
import com.goservi.availability.repository.WeeklyScheduleRepository;
import com.goservi.booking.repository.BookingRepository;
import com.goservi.common.exception.NotFoundException;
import com.goservi.serviceoffer.repository.ServiceOfferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AvailabilityService {

    private final WeeklyScheduleRepository scheduleRepo;
    private final BookingRepository bookingRepo;
    private final ServiceOfferRepository offerRepo;

    // ── CRUD Horario ──────────────────────────────────────────

    public List<AvailabilityDtos.ScheduleResponse> saveSchedules(
            Long serviceOfferId, AvailabilityDtos.BulkScheduleRequest req) {

        if (!offerRepo.existsById(serviceOfferId))
            throw new NotFoundException("Servicio no encontrado");

        List<AvailabilityDtos.ScheduleResponse> results = new ArrayList<>();

        for (var s : req.getSchedules()) {
            var existing = scheduleRepo.findByServiceOfferIdAndDayOfWeek(serviceOfferId, s.getDayOfWeek());
            WeeklySchedule schedule;
            if (existing.isPresent()) {
                schedule = existing.get();
                schedule.setStartTime(LocalTime.parse(s.getStartTime()));
                schedule.setEndTime(LocalTime.parse(s.getEndTime()));
                schedule.setActive(s.isActive());
            } else {
                schedule = WeeklySchedule.builder()
                        .serviceOfferId(serviceOfferId)
                        .dayOfWeek(s.getDayOfWeek())
                        .startTime(LocalTime.parse(s.getStartTime()))
                        .endTime(LocalTime.parse(s.getEndTime()))
                        .active(s.isActive())
                        .build();
            }
            results.add(toResponse(scheduleRepo.save(schedule)));
        }
        return results;
    }

    public List<AvailabilityDtos.ScheduleResponse> getSchedule(Long serviceOfferId) {
        return scheduleRepo.findByServiceOfferIdAndActiveTrue(serviceOfferId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Generación de Slots ───────────────────────────────────

    public List<AvailabilityDtos.TimeSlotDTO> getSlots(
            Long serviceOfferId,
            String startDateStr,
            String endDateStr,
            int durationMinutes) {

        var offer = offerRepo.findById(serviceOfferId)
                .orElseThrow(() -> new NotFoundException("Servicio no encontrado"));

        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        var schedules = scheduleRepo.findByServiceOfferIdAndActiveTrue(serviceOfferId);
        Map<DayOfWeek, WeeklySchedule> scheduleByDay = schedules.stream()
                .collect(Collectors.toMap(WeeklySchedule::getDayOfWeek, s -> s));

        var existingBookings = bookingRepo.findByServiceOfferIdAndDateRange(
                serviceOfferId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

        LocalDateTime minStart = LocalDateTime.now().plusMinutes(offer.getLeadTimeMin());

        List<AvailabilityDtos.TimeSlotDTO> slots = new ArrayList<>();

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            DayOfWeek day = mapJavaDay(current.getDayOfWeek());
            WeeklySchedule schedule = scheduleByDay.get(day);

            if (schedule != null && schedule.isActive()) {
                LocalDateTime windowStart = LocalDateTime.of(current, schedule.getStartTime());
                LocalDateTime windowEnd   = LocalDateTime.of(current, schedule.getEndTime());

                LocalDateTime cursor = windowStart;
                while (!cursor.plusMinutes(durationMinutes).isAfter(windowEnd)) {
                    // Copias efectivamente finales para el lambda
                    final LocalDateTime fs = cursor;
                    final LocalDateTime fe = cursor.plusMinutes(durationMinutes);

                    if (fs.isAfter(minStart)) {
                        boolean overlaps = existingBookings.stream().anyMatch(b ->
                                fs.isBefore(b.getEndLocal()) && fe.isAfter(b.getStartLocal()));

                        if (!overlaps) {
                            slots.add(AvailabilityDtos.TimeSlotDTO.builder()
                                    .startLocal(fs)
                                    .endLocal(fe)
                                    .instantBook(offer.isInstantBookingEnabled())
                                    .build());
                        }
                    }
                    cursor = cursor.plusMinutes(30);
                }
            }
            current = current.plusDays(1);
        }
        return slots;
    }

    // ── Helpers ───────────────────────────────────────────────

    private DayOfWeek mapJavaDay(java.time.DayOfWeek javaDay) {
        return switch (javaDay) {
            case MONDAY    -> DayOfWeek.MONDAY;
            case TUESDAY   -> DayOfWeek.TUESDAY;
            case WEDNESDAY -> DayOfWeek.WEDNESDAY;
            case THURSDAY  -> DayOfWeek.THURSDAY;
            case FRIDAY    -> DayOfWeek.FRIDAY;
            case SATURDAY  -> DayOfWeek.SATURDAY;
            case SUNDAY    -> DayOfWeek.SUNDAY;
        };
    }

    private AvailabilityDtos.ScheduleResponse toResponse(WeeklySchedule s) {
        return AvailabilityDtos.ScheduleResponse.builder()
                .id(s.getId())
                .serviceOfferId(s.getServiceOfferId())
                .dayOfWeek(s.getDayOfWeek())
                .startTime(s.getStartTime().toString())
                .endTime(s.getEndTime().toString())
                .active(s.isActive())
                .build();
    }
}
