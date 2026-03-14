package com.goservi.chat.repository;

import com.goservi.chat.entity.ChatThread;
import com.goservi.chat.entity.ThreadKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatThreadRepository extends JpaRepository<ChatThread, Long> {
    List<ChatThread> findByClientIdOrProfessionalId(Long clientId, Long professionalId);
    Optional<ChatThread> findByClientIdAndProfessionalIdAndServiceOfferIdAndKind(
            Long clientId, Long professionalId, Long serviceOfferId, ThreadKind kind);
    Optional<ChatThread> findByBookingId(String bookingId);
    
}
