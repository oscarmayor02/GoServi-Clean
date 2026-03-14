package com.goservi.user.repository;

import com.goservi.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByAuthUserId(Long authUserId);
    boolean existsByAuthUserId(Long authUserId);

    @Query(value = """
        SELECT *, (
            6371 * acos(
                cos(radians(:lat)) * cos(radians(latitude)) *
                cos(radians(longitude) - radians(:lng)) +
                sin(radians(:lat)) * sin(radians(latitude))
            )
        ) AS distance
        FROM user_profiles
        WHERE latitude IS NOT NULL AND longitude IS NOT NULL
        HAVING distance < :radiusKm
        ORDER BY distance
        LIMIT :limit
        """, nativeQuery = true)
    List<UserProfile> findNearby(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm,
            @Param("limit") int limit);
}
