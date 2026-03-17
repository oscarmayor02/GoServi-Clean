package com.goservi.payment.repository;

import com.goservi.payment.entity.Payment;
import com.goservi.payment.entity.PaymentMethod;
import com.goservi.payment.entity.PaymentStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByBookingId(String bookingId);
    Optional<Payment> findByWompiTransactionId(String transactionId);
    List<Payment> findAll(Sort sort);
    List<Payment> findByClientIdAndPaymentMethodAndStatus(
            Long clientId, PaymentMethod method, PaymentStatus status);

    List<Payment> findByProfessionalIdAndPaymentMethodAndStatus(
            Long professionalId, PaymentMethod method, PaymentStatus status);
    List<Payment> findByPaymentMethodAndStatus(PaymentMethod method, PaymentStatus status);

}
