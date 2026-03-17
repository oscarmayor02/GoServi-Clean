package com.goservi.review.repository;

import com.goservi.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByReviewedId(Long reviewedId);
    boolean existsByBookingIdAndReviewerId(String bookingId, Long reviewerId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewedId = :userId")
    Double getAverageRating(@Param("userId") Long userId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.reviewedId = :userId")
    Long getReviewCount(@Param("userId") Long userId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewerId = :reviewerId")
    Double getAverageRatingByReviewer(@Param("reviewerId") Long reviewerId);
}
