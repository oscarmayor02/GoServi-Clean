package com.goservi.availability.repository;

import com.goservi.availability.entity.DayOfWeek;
import com.goservi.availability.entity.WeeklySchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WeeklyScheduleRepository extends JpaRepository<WeeklySchedule, Long> {
    List<WeeklySchedule> findByServiceOfferIdAndActiveTrue(Long serviceOfferId);
    Optional<WeeklySchedule> findByServiceOfferIdAndDayOfWeek(Long serviceOfferId, DayOfWeek day);
}
