package com.goservi.payment.service;

import com.goservi.payment.dto.PaymentDtos;
import java.util.List;

public interface PaymentService {
    PaymentDtos.PaymentResponse initiate(Long clientId, PaymentDtos.CreatePaymentRequest req);
    PaymentDtos.PaymentResponse getByBooking(String bookingId);
    void handleWompiWebhook(PaymentDtos.WompiWebhookEvent event, String signature);

    PaymentDtos.EarningsResponse getEarnings(Long professionalId);
    PaymentDtos.WithdrawalResponse requestWithdrawal(Long professionalId, PaymentDtos.WithdrawalRequest req);
    List<PaymentDtos.WithdrawalResponse> getWithdrawals(Long professionalId);
    List<PaymentDtos.PaymentHistoryItem> getClientHistory(Long clientId);  // ← NUEVO
    /** Admin marca un pago en efectivo como pagado */
    PaymentDtos.PaymentResponse adminMarkPaid(String paymentId);
    /** Admin aprueba pago en efectivo (comisión verificada) */
    PaymentDtos.PaymentResponse approveCashPayment(String paymentId);

    /** Admin rechaza / marca como no pagado */
    PaymentDtos.PaymentResponse rejectCashPayment(String paymentId, String reason);

    /** Lista de pagos en efectivo pendientes de verificación */
    List<PaymentDtos.CashPaymentAdminView> getPendingCashPayments();
}