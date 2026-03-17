package com.goservi.serviceoffer.service.impl;

import com.goservi.booking.repository.BookingRepository;
import com.goservi.common.exception.BadRequestException;
import com.goservi.common.exception.NotFoundException;
import com.goservi.serviceoffer.dto.ServiceOfferDtos;
import com.goservi.serviceoffer.entity.Category;
import com.goservi.serviceoffer.entity.ServiceOffer;
import com.goservi.serviceoffer.repository.CategoryRepository;
import com.goservi.serviceoffer.repository.ServiceOfferRepository;
import com.goservi.serviceoffer.repository.SubcategoryRepository;
import com.goservi.serviceoffer.service.ServiceOfferService;
import com.goservi.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ServiceOfferServiceImpl implements ServiceOfferService {

    private final ServiceOfferRepository repo;
    private final CategoryRepository categoryRepo;
    private final SubcategoryRepository subcategoryRepo;
    private final UserProfileService userProfileService;
    private final BookingRepository bookingRepo;
    @Override
    public ServiceOfferDtos.ServiceOfferResponse create(Long userId, ServiceOfferDtos.ServiceOfferRequest req) {
        Category cat = categoryRepo.findById(req.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));

        com.goservi.serviceoffer.entity.Subcategory subcat = null;
        if (req.getSubcategoryId() != null) {
            subcat = subcategoryRepo.findById(req.getSubcategoryId())
                    .orElseThrow(() -> new NotFoundException("Subcategoría no encontrada"));
        }

        ServiceOffer offer = ServiceOffer.builder()
                .userId(userId)
                .category(cat)
                .subcategory(subcat)
                .title(req.getTitle())
                .description(req.getDescription())
                .pricePerHour(req.getPricePerHour())
                .discountPercent(req.getDiscountPercent())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .city(req.getCity())
                .photoUrls(req.getPhotoUrls() != null ? req.getPhotoUrls() : new java.util.ArrayList<>())
                .experienceYears(req.getExperienceYears())
                .instantBookingEnabled(req.isInstantBookingEnabled())
                .leadTimeMin(req.getLeadTimeMin() != null ? req.getLeadTimeMin() : 15)
                .minDurationMin(req.getMinDurationMin() != null ? req.getMinDurationMin() : 60)
                .maxDurationMin(req.getMaxDurationMin() != null ? req.getMaxDurationMin() : 480)
                .dailyCapacity(req.getDailyCapacity() != null ? req.getDailyCapacity() : 3)
                .build();

        var saved = repo.save(offer);

        try { userProfileService.markFirstAd(userId); } catch (Exception e) {
            log.warn("Could not mark first ad: {}", e.getMessage());
        }

        return toResponse(saved);
    }

    @Override
    public ServiceOfferDtos.ServiceOfferResponse update(Long id, Long userId, ServiceOfferDtos.ServiceOfferRequest req) {
        ServiceOffer offer = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Servicio no encontrado"));
        if (!offer.getUserId().equals(userId)) throw new BadRequestException("No tienes permiso para editar este servicio");

        if (req.getCategoryId() != null) {
            Category cat = categoryRepo.findById(req.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));
            offer.setCategory(cat);
        }
        if (req.getTitle() != null) offer.setTitle(req.getTitle());
        if (req.getDescription() != null) offer.setDescription(req.getDescription());
        if (req.getPricePerHour() != null) offer.setPricePerHour(req.getPricePerHour());
        if (req.getDiscountPercent() != null) offer.setDiscountPercent(req.getDiscountPercent());
        if (req.getLatitude() != null) offer.setLatitude(req.getLatitude());
        if (req.getLongitude() != null) offer.setLongitude(req.getLongitude());
        if (req.getCity() != null) offer.setCity(req.getCity());
        if (req.getPhotoUrls() != null) offer.setPhotoUrls(req.getPhotoUrls());
        if (req.getExperienceYears() != null) offer.setExperienceYears(req.getExperienceYears());

        return toResponse(repo.save(offer));
    }

    @Override
    public ServiceOfferDtos.ServiceOfferResponse getById(Long id) {
        return repo.findById(id).map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Servicio no encontrado"));
    }

    @Override
    public List<ServiceOfferDtos.ServiceOfferResponse> getByUser(Long userId) {
        return repo.findByUserIdAndActiveTrue(userId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<ServiceOfferDtos.ServiceOfferResponse> searchNearby(
            double lat, double lng, double radiusKm, Long categoryId, Long subcategoryId) {

        List<Object[]> rows = repo.findNearbyWithDistance(lat, lng, radiusKm, categoryId, subcategoryId);
        log.info("Nearby rows: {}", rows.size());

        // Índices: 0=id, 1=user_id, 2=category_id, 3=subcategory_id,
        // 4=title, 5=description, 6=price_per_hour, 7=discount_percent,
        // 8=latitude, 9=longitude, 10=city, 11=experience_years,
        // 12=active, 13=instant_booking_enabled,
        // 14=lead_time_min, 15=min_duration_min, 16=max_duration_min, 17=daily_capacity,
        // 18=distance_km

        return rows.stream().map(row -> {
            ServiceOfferDtos.ServiceOfferResponse res = ServiceOfferDtos.ServiceOfferResponse.builder()
                    .id(row[0] != null ? ((Number) row[0]).longValue() : null)
                    .userId(row[1] != null ? ((Number) row[1]).longValue() : null)
                    .categoryId(row[2] != null ? ((Number) row[2]).longValue() : null)
                    .subcategoryId(row[3] != null ? ((Number) row[3]).longValue() : null)
                    .title(row[4] != null ? row[4].toString() : null)
                    .description(row[5] != null ? row[5].toString() : null)
                    .pricePerHour(row[6] != null ? new java.math.BigDecimal(row[6].toString()) : null)
                    .discountPercent(row[7] != null ? ((Number) row[7]).intValue() : null)
                    .latitude(row[8] != null ? ((Number) row[8]).doubleValue() : null)
                    .longitude(row[9] != null ? ((Number) row[9]).doubleValue() : null)
                    .city(row[10] != null ? row[10].toString() : null)
                    .photoUrls(new java.util.ArrayList<>()) // se llena aparte
                    .experienceYears(row[11] != null ? ((Number) row[11]).intValue() : null)
                    .active(row[12] != null && (Boolean) row[12])
                    .instantBookingEnabled(row[13] != null && (Boolean) row[13])
                    .leadTimeMin(row[14] != null ? ((Number) row[14]).intValue() : null)
                    .minDurationMin(row[15] != null ? ((Number) row[15]).intValue() : null)
                    .maxDurationMin(row[16] != null ? ((Number) row[16]).intValue() : null)
                    .dailyCapacity(row[17] != null ? ((Number) row[17]).intValue() : null)
                    .distanceKm(row[18] != null ? ((Number) row[18]).doubleValue() : null)
                    .build();

            // Perfil del profesional
            try {
                var profile = userProfileService.getById(res.getUserId());
                res.setProfessionalName(profile.getFullName());
                res.setProfessionalPhoto(profile.getPhotoUrl());
            } catch (Exception e) {
                log.warn("Sin perfil para userId {}", res.getUserId());
            }

            // Nombres de categoría/subcategoría
            try {
                categoryRepo.findById(res.getCategoryId())
                        .ifPresent(c -> res.setCategoryName(c.getName()));
            } catch (Exception ignored) {}
            try {
                if (res.getSubcategoryId() != null)
                    subcategoryRepo.findById(res.getSubcategoryId())
                            .ifPresent(s -> res.setSubcategoryName(s.getName()));
            } catch (Exception ignored) {}

            return res;
        }).collect(Collectors.toList());
    }

    @Override
    public void deactivate(Long id, Long userId) {
        ServiceOffer offer = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Servicio no encontrado"));
        if (!offer.getUserId().equals(userId)) throw new BadRequestException("No tienes permiso");
        offer.setActive(false);
        repo.save(offer);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public ServiceOfferDtos.ServiceOfferResponse addPhoto(Long id, Long userId, String photoUrl) {
        ServiceOffer offer = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Servicio no encontrado"));
        if (!offer.getUserId().equals(userId)) throw new BadRequestException("No tienes permiso");
        offer.getPhotoUrls().add(photoUrl);
        return toResponse(repo.save(offer));
    }

    private ServiceOfferDtos.ServiceOfferResponse toResponse(ServiceOffer o) {
        // Contar bookings PAID + COMPLETED de este profesional
        long completed = 0;
        try {
            completed = bookingRepo.countByProfessionalIdAndStatusIn(
                    o.getUserId(),
                    List.of(com.goservi.booking.entity.BookingStatus.COMPLETED,
                            com.goservi.booking.entity.BookingStatus.PAID)
            );
        } catch (Exception ignored) {}

        return ServiceOfferDtos.ServiceOfferResponse.builder()
                .id(o.getId())
                .userId(o.getUserId())
                .categoryId(o.getCategory() != null ? o.getCategory().getId() : null)
                .categoryName(o.getCategory() != null ? o.getCategory().getName() : null)
                .subcategoryId(o.getSubcategory() != null ? o.getSubcategory().getId() : null)
                .subcategoryName(o.getSubcategory() != null ? o.getSubcategory().getName() : null)
                .title(o.getTitle())
                .description(o.getDescription())
                .pricePerHour(o.getPricePerHour())
                .discountPercent(o.getDiscountPercent())
                .latitude(o.getLatitude())
                .longitude(o.getLongitude())
                .city(o.getCity())
                .photoUrls(o.getPhotoUrls())
                .experienceYears(o.getExperienceYears())
                .instantBookingEnabled(o.isInstantBookingEnabled())
                .leadTimeMin(o.getLeadTimeMin())
                .minDurationMin(o.getMinDurationMin())
                .maxDurationMin(o.getMaxDurationMin())
                .dailyCapacity(o.getDailyCapacity())
                .active(o.isActive())
                .completedServices(completed)
                .build();
    }
}
