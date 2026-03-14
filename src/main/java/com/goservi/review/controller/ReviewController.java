package com.goservi.review.controller;

import com.goservi.auth.repository.AuthUserRepository;
import com.goservi.common.exception.NotFoundException;
import com.goservi.review.dto.ReviewDtos;
import com.goservi.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Reseñas y calificaciones")
public class ReviewController {

    private final ReviewService reviewService;
    private final AuthUserRepository authRepo;

    @Operation(summary = "Dejar reseña")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewDtos.ReviewResponse> create(
            @Valid @RequestBody ReviewDtos.ReviewRequest req,
            Principal principal) {
        Long userId = getAuthUserId(principal);
        return ResponseEntity.ok(reviewService.create(userId, req));
    }

    @Operation(summary = "Ver reseñas de un usuario (alias /professional)")
    @GetMapping("/professional/{userId}")
    public ResponseEntity<ReviewDtos.RatingSummary> getSummaryAlias(@PathVariable Long userId) {
        return ResponseEntity.ok(reviewService.getSummary(userId));
    }

    @Operation(summary = "Ver reseñas de un usuario")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ReviewDtos.RatingSummary> getSummary(@PathVariable Long userId) {
        return ResponseEntity.ok(reviewService.getSummary(userId));
    }

    @Operation(summary = "Rating summary rápido (alias /rating)")
    @GetMapping("/user/{userId}/rating")
    public ResponseEntity<ReviewDtos.RatingSummary> getRating(@PathVariable Long userId) {
        return ResponseEntity.ok(reviewService.getSummary(userId));
    }

    private Long getAuthUserId(Principal principal) {
        return authRepo.findByEmail(principal.getName())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"))
                .getId();
    }
}
