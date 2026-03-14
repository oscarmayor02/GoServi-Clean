package com.goservi.review.service;

import com.goservi.review.dto.ReviewDtos;

public interface ReviewService {
    ReviewDtos.ReviewResponse create(Long reviewerId, ReviewDtos.ReviewRequest req);
    ReviewDtos.RatingSummary getSummary(Long userId);
}
