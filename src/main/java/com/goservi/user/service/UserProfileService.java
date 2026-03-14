package com.goservi.user.service;

import com.goservi.user.dto.UserDtos;

import java.util.List;

public interface UserProfileService {
    UserDtos.ProfileResponse getOrCreateProfile(Long authUserId);
    UserDtos.ProfileResponse updateProfile(Long authUserId, UserDtos.ProfileRequest req);
    UserDtos.ProfileResponse updatePhoto(Long authUserId, String photoUrl);
    UserDtos.ProfileResponse getById(Long authUserId);
    void markFirstAd(Long authUserId);
    void markOnboardingSeen(Long authUserId);
    List<UserDtos.ProfileResponse> searchNearby(double lat, double lng, double radiusKm);
    com.goservi.common.dto.UserSummary getSummary(Long authUserId);
}