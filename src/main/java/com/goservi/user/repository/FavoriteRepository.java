package com.goservi.user.repository;

import com.goservi.user.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByClientId(Long clientId);
    Optional<Favorite> findByClientIdAndProfessionalIdAndServiceOfferId(Long clientId, Long professionalId, Long serviceOfferId);
    boolean existsByClientIdAndProfessionalIdAndServiceOfferId(Long clientId, Long professionalId, Long serviceOfferId);
    void deleteByClientIdAndProfessionalId(Long clientId, Long professionalId);
    void deleteByClientIdAndProfessionalIdAndServiceOfferId(Long clientId, Long professionalId, Long serviceOfferId);
}