package com.goservi.review.service.impl;

import com.goservi.booking.entity.BookingStatus;
import com.goservi.booking.repository.BookingRepository;
import com.goservi.common.exception.BadRequestException;
import com.goservi.common.exception.NotFoundException;
import com.goservi.review.dto.ReviewDtos;
import com.goservi.review.entity.Review;
import com.goservi.review.repository.ReviewRepository;
import com.goservi.review.service.ReviewService;
import com.goservi.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepo;
    private final BookingRepository bookingRepo;
    private final UserProfileService userProfileService;

    @Override
    public ReviewDtos.ReviewResponse create(Long reviewerId, ReviewDtos.ReviewRequest req) {
        var booking = bookingRepo.findById(req.getBookingId())
                .orElseThrow(() -> new NotFoundException("Reserva no encontrada"));

        if (booking.getStatus() != BookingStatus.PAID && booking.getStatus() != BookingStatus.COMPLETED)
            throw new BadRequestException("Solo se puede reseñar una reserva completada o pagada");

        if (!booking.getClientId().equals(reviewerId) && !booking.getProfessionalId().equals(reviewerId))
            throw new BadRequestException("No participaste en esta reserva");

        if (reviewRepo.existsByBookingIdAndReviewerId(req.getBookingId(), reviewerId))
            throw new BadRequestException("Ya dejaste una reseña para esta reserva");

        Long reviewedId = booking.getClientId().equals(reviewerId)
                ? booking.getProfessionalId() : booking.getClientId();

        Review review = Review.builder()
                .bookingId(req.getBookingId())
                .reviewerId(reviewerId)
                .reviewedId(reviewedId)
                .rating(req.getRating())
                .comment(req.getComment())
                .build();

        return toResponse(reviewRepo.save(review));
    }

    @Override
    public ReviewDtos.RatingSummary getSummary(Long userId) {
        var reviews = reviewRepo.findByReviewedId(userId).stream()
                .map(this::toResponse).collect(Collectors.toList());
        return ReviewDtos.RatingSummary.builder()
                .userId(userId)
                .averageRating(reviewRepo.getAverageRating(userId))
                .reviewCount(reviewRepo.getReviewCount(userId))
                .reviews(reviews)
                .build();
    }

    private ReviewDtos.ReviewResponse toResponse(Review r) {
        String name = null, photo = null;
        try {
            var s = userProfileService.getSummary(r.getReviewerId());
            name = s.getFullName(); photo = s.getPhotoUrl();
        } catch (Exception ignored) {}

        return ReviewDtos.ReviewResponse.builder()
                .id(r.getId())
                .bookingId(r.getBookingId())
                .reviewerId(r.getReviewerId())
                .reviewerName(name)
                .reviewerPhoto(photo)
                .reviewedId(r.getReviewedId())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
