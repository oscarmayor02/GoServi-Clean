
// ── WithdrawalRequestRepository.java ────────────────────────────
package com.goservi.payment.repository;

import com.goservi.payment.entity.WithdrawalRequest;
import com.goservi.payment.entity.WithdrawalStatus;
import org.springdoc.core.converters.models.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, String> {
    List<WithdrawalRequest> findByProfessionalIdOrderByCreatedAtDesc(Long professionalId);
    List<WithdrawalRequest> findByStatus(WithdrawalStatus status);

    List<WithdrawalRequest> findByProfessionalId(Long professionalId);
}