package com.goservi.tracking.repository;

import com.goservi.tracking.entity.TrackingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TrackingLogRepository extends JpaRepository<TrackingLog, Long> {
    Optional<TrackingLog> findTopByBookingIdOrderByRecordedAtDesc(String bookingId);

    @Modifying
    @Transactional
    @Query("DELETE FROM TrackingLog t WHERE t.recordedAt < :before")
    void deleteOlderThan(@Param("before") LocalDateTime before);
}
