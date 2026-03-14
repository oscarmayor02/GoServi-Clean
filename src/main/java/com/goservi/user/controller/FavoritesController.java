package com.goservi.user.controller;

import com.goservi.user.entity.Favorite;
import com.goservi.user.repository.FavoriteRepository;
import com.goservi.serviceoffer.repository.ServiceOfferRepository;
import com.goservi.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoritesController {

    private final FavoriteRepository favoriteRepo;
    private final ServiceOfferRepository serviceOfferRepo;
    private final UserProfileService userProfileService;

    // ─── DTO de respuesta enriquecido ───────────────────────────────────────
    public record FavoriteResponse(
            Long id,
            Long clientId,
            Long professionalId,
            Long serviceOfferId,
            // Datos del profesional
            String professionalName,
            String professionalPhoto,
            Double professionalRating,
            // Datos de la oferta
            String serviceName,
            BigDecimal servicePrice,
            String city,
            LocalDateTime createdAt
    ) {}

    // GET /favorites/{clientId}
    @GetMapping("/{clientId}")
    public ResponseEntity<List<FavoriteResponse>> getFavorites(@PathVariable Long clientId) {
        List<Favorite> favs = favoriteRepo.findByClientId(clientId);

        List<FavoriteResponse> enriched = favs.stream().map(fav -> {
            String professionalName = null;
            String professionalPhoto = null;
            String serviceName = null;
            BigDecimal servicePrice = null;
            String city = null;

            // Enriquecer con datos del profesional
            try {
                var profile = userProfileService.getSummary(fav.getProfessionalId());
                professionalName = profile.getFullName();
                professionalPhoto = profile.getPhotoUrl();
                city = null; // UserSummary no tiene city, se toma de la oferta
            } catch (Exception e) {
                log.warn("Sin perfil para professionalId {}: {}", fav.getProfessionalId(), e.getMessage());
            }

            // Enriquecer con datos de la oferta
            if (fav.getServiceOfferId() != null) {
                try {
                    var offer = serviceOfferRepo.findById(fav.getServiceOfferId()).orElse(null);
                    if (offer != null) {
                        serviceName  = offer.getTitle();
                        servicePrice = offer.getPricePerHour();
                        city         = offer.getCity();
                    }
                } catch (Exception e) {
                    log.warn("Sin oferta para serviceOfferId {}: {}", fav.getServiceOfferId(), e.getMessage());
                }
            }

            return new FavoriteResponse(
                    fav.getId(),
                    fav.getClientId(),
                    fav.getProfessionalId(),
                    fav.getServiceOfferId(),
                    professionalName,
                    professionalPhoto,
                    null,           // rating: agregar cuando tengas el módulo de reviews
                    serviceName,
                    servicePrice,
                    city,
                    fav.getCreatedAt()
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(enriched);
    }

    // POST /favorites  body: { clientId, professionalId, serviceOfferId }
    @PostMapping
    public ResponseEntity<Favorite> addFavorite(@RequestBody Map<String, Object> body) {
        Long clientId       = Long.valueOf(body.get("clientId").toString());
        Long professionalId = Long.valueOf(body.get("professionalId").toString());
        Long serviceOfferId = body.get("serviceOfferId") != null
                ? Long.valueOf(body.get("serviceOfferId").toString())
                : null;

        // Evitar duplicados por (cliente, profesional, oferta)
        if (serviceOfferId != null &&
                favoriteRepo.existsByClientIdAndProfessionalIdAndServiceOfferId(clientId, professionalId, serviceOfferId)) {
            return ResponseEntity.ok(
                    favoriteRepo.findByClientIdAndProfessionalIdAndServiceOfferId(clientId, professionalId, serviceOfferId)
                            .orElseThrow()
            );
        }

        Favorite fav = Favorite.builder()
                .clientId(clientId)
                .professionalId(professionalId)
                .serviceOfferId(serviceOfferId)
                .build();

        return ResponseEntity.ok(favoriteRepo.save(fav));
    }

    // DELETE /favorites?clientId=X&professionalId=Y
    // DELETE /favorites?clientId=X&professionalId=Y&serviceOfferId=Z
    @Transactional
    @DeleteMapping
    public ResponseEntity<Void> removeFavorite(
            @RequestParam Long clientId,
            @RequestParam Long professionalId,
            @RequestParam(required = false) Long serviceOfferId) {

        if (serviceOfferId != null) {
            favoriteRepo.deleteByClientIdAndProfessionalIdAndServiceOfferId(clientId, professionalId, serviceOfferId);
        } else {
            favoriteRepo.deleteByClientIdAndProfessionalId(clientId, professionalId);
        }
        return ResponseEntity.noContent().build();
    }
}