package com.goservi.otp.repository;

import com.goservi.otp.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COUNT(o) FROM OtpCode o WHERE o.userId = :userId AND o.createdAt > :since")
    long countRecentByUser(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Modifying
    @Transactional
    @Query("UPDATE OtpCode o SET o.used = true WHERE o.userId = :userId AND o.used = false")
    void invalidateAllForUser(@Param("userId") Long userId);
}
