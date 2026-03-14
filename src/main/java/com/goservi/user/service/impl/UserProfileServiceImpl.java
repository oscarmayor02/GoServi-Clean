package com.goservi.user.service.impl;

import com.goservi.auth.repository.AuthUserRepository;
import com.goservi.common.dto.UserSummary;
import com.goservi.common.exception.NotFoundException;
import com.goservi.user.dto.UserDtos;
import com.goservi.user.entity.UserProfile;
import com.goservi.user.repository.UserProfileRepository;
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
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository profileRepo;
    private final AuthUserRepository authRepo;

    @Override
    public UserDtos.ProfileResponse getOrCreateProfile(Long authUserId) {
        return profileRepo.findByAuthUserId(authUserId)
                .map(this::toResponse)
                .orElseGet(() -> {
                    var profile = UserProfile.builder().authUserId(authUserId).build();
                    return toResponse(profileRepo.save(profile));
                });
    }

    @Override
    public UserDtos.ProfileResponse updateProfile(Long authUserId, UserDtos.ProfileRequest req) {
        UserProfile profile = profileRepo.findByAuthUserId(authUserId)
                .orElseGet(() -> UserProfile.builder().authUserId(authUserId).build());

        if (req.getFullName() != null) profile.setFullName(req.getFullName());
        if (req.getBio() != null) profile.setBio(req.getBio());
        if (req.getPhone() != null) profile.setPhone(req.getPhone());
        if (req.getLatitude() != null) profile.setLatitude(req.getLatitude());
        if (req.getLongitude() != null) profile.setLongitude(req.getLongitude());
        if (req.getAddress() != null) profile.setAddress(req.getAddress());
        if (req.getCity() != null) profile.setCity(req.getCity());
        if (req.getGender() != null) profile.setGender(req.getGender());
        if (req.getBirthDate() != null) profile.setBirthDate(req.getBirthDate());
        if (req.getDocumentType() != null) profile.setDocumentType(req.getDocumentType());
        if (req.getDocumentNumber() != null) profile.setDocumentNumber(req.getDocumentNumber());
        if (req.getStreet() != null) profile.setStreet(req.getStreet());
        if (req.getStreetNumber() != null) profile.setStreetNumber(req.getStreetNumber());
        if (req.getProvince() != null) profile.setProvince(req.getProvince());
        if (req.getPostalCode() != null) profile.setPostalCode(req.getPostalCode());
        if (req.getCountry() != null) profile.setCountry(req.getCountry());
        if (req.getDepartment() != null) profile.setDepartment(req.getDepartment());
        if (req.getOnboardingSeen() != null) profile.setOnboardingSeen(req.getOnboardingSeen());

        return toResponse(profileRepo.save(profile));
    }

    @Override
    public UserDtos.ProfileResponse updatePhoto(Long authUserId, String photoUrl) {
        UserProfile profile = profileRepo.findByAuthUserId(authUserId)
                .orElseGet(() -> UserProfile.builder().authUserId(authUserId).build());
        profile.setPhotoUrl(photoUrl);
        return toResponse(profileRepo.save(profile));
    }

    @Override
    public UserDtos.ProfileResponse getById(Long authUserId) {
        return profileRepo.findByAuthUserId(authUserId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Perfil no encontrado"));
    }

    @Override
    public void markFirstAd(Long authUserId) {
        profileRepo.findByAuthUserId(authUserId).ifPresent(p -> {
            p.setFirstAdCreated(true);
            profileRepo.save(p);
        });
    }

    @Override
    public void markOnboardingSeen(Long authUserId) {
        UserProfile profile = profileRepo.findByAuthUserId(authUserId)
                .orElseGet(() -> profileRepo.save(
                        UserProfile.builder().authUserId(authUserId).build()
                ));
        profile.setOnboardingSeen(true);
        profileRepo.save(profile);
    }

    @Override
    public List<UserDtos.ProfileResponse> searchNearby(double lat, double lng, double radiusKm) {
        return profileRepo.findNearby(lat, lng, radiusKm, 50)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserSummary getSummary(Long authUserId) {
        var authUser = authRepo.findById(authUserId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        var profile = profileRepo.findByAuthUserId(authUserId)
                .orElseGet(() -> profileRepo.save(
                        UserProfile.builder().authUserId(authUserId).build()
                ));

        return UserSummary.builder()
                .id(authUser.getId())
                .email(authUser.getEmail())
                .fullName(profile.getFullName() != null ? profile.getFullName() : authUser.getName())
                .photoUrl(profile.getPhotoUrl())
                .phone(profile.getPhone() != null ? profile.getPhone() : authUser.getPhone())
                .role(authUser.getActiveRole().name())
                .firstAdCreated(profile.isFirstAdCreated())
                .onboardingSeen(profile.isOnboardingSeen())
                .build();
    }

    private UserDtos.ProfileResponse toResponse(UserProfile p) {
        return UserDtos.ProfileResponse.builder()
                .id(p.getId())
                .authUserId(p.getAuthUserId())
                .fullName(p.getFullName())
                .bio(p.getBio())
                .photoUrl(p.getPhotoUrl())
                .phone(p.getPhone())
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .address(p.getAddress())
                .city(p.getCity())
                .gender(p.getGender())
                .birthDate(p.getBirthDate())
                .documentType(p.getDocumentType())
                .documentNumber(p.getDocumentNumber())
                .street(p.getStreet())
                .streetNumber(p.getStreetNumber())
                .province(p.getProvince())
                .postalCode(p.getPostalCode())
                .country(p.getCountry())
                .department(p.getDepartment())
                .phoneVerified(p.isPhoneVerified())
                .identityVerified(p.isIdentityVerified())
                .firstAdCreated(p.isFirstAdCreated())
                .onboardingSeen(p.isOnboardingSeen())
                .build();
    }
}