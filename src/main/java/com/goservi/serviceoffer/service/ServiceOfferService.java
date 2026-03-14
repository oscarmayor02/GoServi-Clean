package com.goservi.serviceoffer.service;

import com.goservi.serviceoffer.dto.ServiceOfferDtos;

import java.util.List;

public interface ServiceOfferService {
    ServiceOfferDtos.ServiceOfferResponse create(Long userId, ServiceOfferDtos.ServiceOfferRequest req);
    ServiceOfferDtos.ServiceOfferResponse update(Long id, Long userId, ServiceOfferDtos.ServiceOfferRequest req);
    ServiceOfferDtos.ServiceOfferResponse getById(Long id);
    List<ServiceOfferDtos.ServiceOfferResponse> getByUser(Long userId);
    List<ServiceOfferDtos.ServiceOfferResponse> searchNearby(double lat, double lng, double radiusKm, Long categoryId, Long subcategoryId);
    void deactivate(Long id, Long userId);
    boolean existsById(Long id);
    ServiceOfferDtos.ServiceOfferResponse addPhoto(Long id, Long userId, String photoUrl);

}
