package com.goservi.serviceoffer.repository;

import com.goservi.serviceoffer.entity.ServiceOffer;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServiceOfferRepository extends JpaRepository<ServiceOffer, Long> {
    List<ServiceOffer> findByUserIdAndActiveTrue(Long userId);
    List<ServiceOffer> findAll(Sort sort);

    @Query(value = """
        SELECT so.*, (
            6371 * acos(
                cos(radians(:lat)) * cos(radians(so.latitude)) *
                cos(radians(so.longitude) - radians(:lng)) +
                sin(radians(:lat)) * sin(radians(so.latitude))
            )
        ) AS distance
        FROM service_offers so
        WHERE so.active = true
          AND so.latitude IS NOT NULL
          AND so.longitude IS NOT NULL
          AND (:categoryId IS NULL OR so.category_id = :categoryId)
          AND (:subcategoryId IS NULL OR so.subcategory_id = :subcategoryId)
        HAVING distance < :radiusKm
        ORDER BY distance
        LIMIT 50
        """, nativeQuery = true)
    List<ServiceOffer> findNearby(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm,
            @Param("categoryId") Long categoryId,
            @Param("subcategoryId") Long subcategoryId);

    @Query(value = """
    SELECT * FROM (
        SELECT so.id, so.user_id, so.category_id, so.subcategory_id,
               so.title, so.description, so.price_per_hour, so.discount_percent,
               so.latitude, so.longitude, so.city,
               so.experience_years,
               so.active, so.instant_booking_enabled,
               so.lead_time_min, so.min_duration_min, so.max_duration_min, so.daily_capacity,
               (6371 * acos(
                   cos(radians(:lat)) * cos(radians(so.latitude)) *
                   cos(radians(so.longitude) - radians(:lng)) +
                   sin(radians(:lat)) * sin(radians(so.latitude))
               )) AS distance_km
        FROM service_offers so
        WHERE so.active = true
          AND so.latitude IS NOT NULL
          AND so.longitude IS NOT NULL
          AND (:categoryId IS NULL OR so.category_id = :categoryId)
          AND (:subcategoryId IS NULL OR so.subcategory_id = :subcategoryId)
    ) sub
    WHERE sub.distance_km < :radiusKm
    ORDER BY sub.distance_km
    LIMIT 50
    """, nativeQuery = true)
    List<Object[]> findNearbyWithDistance(
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radiusKm") Double radiusKm,
            @Param("categoryId") Long categoryId,
            @Param("subcategoryId") Long subcategoryId
    );
}
