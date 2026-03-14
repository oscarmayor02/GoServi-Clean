// ── ProfessionalEarningRepository.java ──────────────────────────
package com.goservi.payment.repository;

import com.goservi.payment.entity.ProfessionalEarning;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProfessionalEarningRepository extends JpaRepository<ProfessionalEarning, Long> {
    Optional<ProfessionalEarning> findByProfessionalId(Long professionalId);
}

