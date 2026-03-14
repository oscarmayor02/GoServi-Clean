package com.goservi.support.repository;

import com.goservi.support.entity.SupportTicket;
import com.goservi.payment.entity.WithdrawalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    List<SupportTicket> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<SupportTicket> findByStatus(String status);

    List<SupportTicket> findByType(String type);

    long countByStatus(String status);
}