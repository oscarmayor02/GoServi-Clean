package com.goservi.booking.repository;

import com.goservi.booking.entity.Booking;
import com.goservi.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, String> {

    List<Booking> findByClientId(Long clientId);
    List<Booking> findByProfessionalId(Long professionalId);
    List<Booking> findByClientIdAndStatus(Long clientId, BookingStatus status);
    List<Booking> findByProfessionalIdAndStatus(Long professionalId, BookingStatus status);

    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.serviceOfferId = :offerId
          AND b.status IN ('CONFIRMED', 'IN_PROGRESS')
          AND b.startLocal < :end
          AND b.endLocal > :start
        """)
    boolean existsOverlap(
            @Param("offerId") Long offerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.serviceOfferId = :offerId
          AND b.status IN ('CONFIRMED', 'IN_PROGRESS')
          AND CAST(b.startLocal AS date) = :date
        """)
    int countByServiceOfferIdAndDate(
            @Param("offerId") Long offerId,
            @Param("date") LocalDate date);

    List<Booking> findAll(Sort sort);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.serviceOfferId = :offerId
          AND b.status IN ('CONFIRMED', 'IN_PROGRESS')
          AND b.startLocal < :endDate
          AND b.endLocal > :startDate
        """)
    List<Booking> findByServiceOfferIdAndDateRange(
            @Param("offerId") Long offerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
