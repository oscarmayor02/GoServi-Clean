package com.goservi.payment.service.impl;

import com.goservi.auth.repository.AuthUserRepository;
import com.goservi.booking.service.BookingService;
import com.goservi.common.exception.BadRequestException;
import com.goservi.common.exception.NotFoundException;
import com.goservi.payment.dto.PaymentDtos;
import com.goservi.payment.entity.*;
import com.goservi.payment.repository.*;
import com.goservi.payment.service.PaymentService;
import com.goservi.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository             paymentRepo;
    private final ProfessionalEarningRepository earningRepo;
    private final WithdrawalRequestRepository   withdrawalRepo;
    private final BookingService                bookingService;
    private final AuthUserRepository            authRepo;
    private final UserProfileService            userProfileService;
    private final WebClient.Builder             webClientBuilder;

    @Value("${wompi.url:https://sandbox.wompi.co/v1}")
    private String wompiUrl;
    @Value("${wompi.private-key:}")
    private String wompiPrivateKey;

    private static final BigDecimal PLATFORM_FEE_PERCENT = new BigDecimal("0.15");

    // ── INITIATE ──────────────────────────────────────────────────
    @Override
    public PaymentDtos.PaymentResponse initiate(Long clientId, PaymentDtos.CreatePaymentRequest req) {
        var booking = bookingService.getById(req.getBookingId());

        if (!booking.getClientId().equals(clientId))
            throw new BadRequestException("Solo el cliente puede iniciar el pago");
        if (booking.getStatus().name().equals("PAID"))
            throw new BadRequestException("Esta reserva ya fue pagada");

        BigDecimal amount = booking.getTotalPrice();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new BadRequestException("El servicio no tiene un precio calculado válido");

        var existing = paymentRepo.findByBookingId(req.getBookingId());
        if (existing.isPresent()) {
            var p = existing.get();
            if (p.getStatus() == PaymentStatus.PAID)
                throw new BadRequestException("Esta reserva ya fue pagada");
            if (p.getStatus() == PaymentStatus.PENDING && p.getPaymentLink() != null)
                return toResponse(p);
        }

        BigDecimal fee    = amount.multiply(PLATFORM_FEE_PERCENT).setScale(2, RoundingMode.HALF_UP);
        BigDecimal proAmt = amount.subtract(fee);

        Payment payment = Payment.builder()
                .bookingId(req.getBookingId())
                .clientId(clientId)
                .professionalId(booking.getProfessionalId())
                .amount(amount)
                .platformFee(fee)
                .professionalAmount(proAmt)
                .status(PaymentStatus.PENDING)
                .build();
        payment = paymentRepo.save(payment);

        try {
            payment.setPaymentLink(generateWompiLink(payment));
        } catch (Exception e) {
            log.warn("Wompi link generation failed: {}", e.getMessage());
        }

        return toResponse(paymentRepo.save(payment));
    }

    // ── GET BY BOOKING ────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public PaymentDtos.PaymentResponse getByBooking(String bookingId) {
        return paymentRepo.findByBookingId(bookingId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Pago no encontrado"));
    }

    // ── WEBHOOK ───────────────────────────────────────────────────
    @Override
    public void handleWompiWebhook(PaymentDtos.WompiWebhookEvent event, String signature) {
        log.info("Wompi webhook: event={}", event.getEvent());
        if (event.getData() == null || event.getData().getTransaction() == null) return;
        var tx = event.getData().getTransaction();

        if ("APPROVED".equals(tx.getStatus())) {
            paymentRepo.findByBookingId(tx.getReference()).ifPresent(payment -> {
                if (payment.getStatus() == PaymentStatus.PAID) return;
                payment.setStatus(PaymentStatus.PAID);
                payment.setWompiTransactionId(tx.getId());
                payment.setPaidAt(LocalDateTime.now());
                paymentRepo.save(payment);
                creditProfessional(payment);
                try { bookingService.markPaidInternal(payment.getBookingId()); }
                catch (Exception e) { log.warn("Could not mark booking as paid: {}", e.getMessage()); }
            });
        } else if ("DECLINED".equals(tx.getStatus()) || "ERROR".equals(tx.getStatus())) {
            paymentRepo.findByBookingId(tx.getReference()).ifPresent(payment -> {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepo.save(payment);
            });
        }
    }

    // ── EARNINGS ──────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public PaymentDtos.EarningsResponse getEarnings(Long professionalId) {
        var earning = earningRepo.findByProfessionalId(professionalId)
                .orElse(ProfessionalEarning.builder().professionalId(professionalId).build());

        List<PaymentDtos.PaymentSummary> recent = paymentRepo
                .findAll(Sort.by(Sort.Direction.DESC, "paidAt"))
                .stream()
                .filter(p -> professionalId.equals(p.getProfessionalId())
                        && p.getStatus() == PaymentStatus.PAID)
                .limit(20)
                .map(p -> PaymentDtos.PaymentSummary.builder()
                        .bookingId(p.getBookingId())
                        .amount(p.getAmount())
                        .platformFee(p.getPlatformFee())
                        .professionalAmount(p.getProfessionalAmount())
                        .paidAt(p.getPaidAt())
                        .build())
                .collect(Collectors.toList());

        return PaymentDtos.EarningsResponse.builder()
                .totalEarned(earning.getTotalEarned())
                .availableBalance(earning.getAvailableBalance())
                .pendingWithdrawal(earning.getPendingWithdrawal())
                .totalWithdrawn(earning.getTotalWithdrawn())
                .recentPayments(recent)
                .build();
    }

    // ── WITHDRAWAL ────────────────────────────────────────────────
    @Override
    public PaymentDtos.WithdrawalResponse requestWithdrawal(Long professionalId,
                                                            PaymentDtos.WithdrawalRequest req) {
        var earning = earningRepo.findByProfessionalId(professionalId)
                .orElseThrow(() -> new BadRequestException("No tienes saldo disponible"));

        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new BadRequestException("El monto debe ser mayor a 0");
        if (earning.getAvailableBalance().compareTo(req.getAmount()) < 0)
            throw new BadRequestException("Saldo insuficiente. Disponible: " + earning.getAvailableBalance());

        earning.setAvailableBalance(earning.getAvailableBalance().subtract(req.getAmount()));
        earning.setPendingWithdrawal(earning.getPendingWithdrawal().add(req.getAmount()));
        earningRepo.save(earning);

        var withdrawal = com.goservi.payment.entity.WithdrawalRequest.builder()
                .professionalId(professionalId)
                .amount(req.getAmount())
                .bankName(req.getBankName())
                .accountNumber(req.getAccountNumber())
                .accountHolder(req.getAccountHolder())
                .status(WithdrawalStatus.PENDING)
                .build();

        return toWithdrawalResponse(withdrawalRepo.save(withdrawal));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentDtos.WithdrawalResponse> getWithdrawals(Long professionalId) {
        return withdrawalRepo.findByProfessionalId(professionalId)
                .stream()
                .map(this::toWithdrawalResponse)
                .collect(Collectors.toList());
    }

    // ── HISTORIAL CLIENTE ─────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<PaymentDtos.PaymentHistoryItem> getClientHistory(Long clientId) {
        return paymentRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .filter(p -> clientId.equals(p.getClientId()))
                .map(p -> PaymentDtos.PaymentHistoryItem.builder()
                        .id(p.getId())
                        .bookingId(p.getBookingId())
                        .professionalName(safeGetName(p.getProfessionalId()))
                        .amount(p.getAmount())
                        .platformFee(p.getPlatformFee())
                        .professionalAmount(p.getProfessionalAmount())
                        .status(p.getStatus().name())
                        .wompiTransactionId(p.getWompiTransactionId())
                        .createdAt(p.getCreatedAt())
                        .paidAt(p.getPaidAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ── PRIVATE ───────────────────────────────────────────────────
    private void creditProfessional(Payment payment) {
        var earning = earningRepo.findByProfessionalId(payment.getProfessionalId())
                .orElseGet(() -> ProfessionalEarning.builder()
                        .professionalId(payment.getProfessionalId())
                        .build());
        earning.setTotalEarned(earning.getTotalEarned().add(payment.getProfessionalAmount()));
        earning.setAvailableBalance(earning.getAvailableBalance().add(payment.getProfessionalAmount()));
        earningRepo.save(earning);
        log.info("Credited {} to professional {} (booking {})",
                payment.getProfessionalAmount(), payment.getProfessionalId(), payment.getBookingId());
    }

    private String generateWompiLink(Payment payment) {
        WebClient client = webClientBuilder.baseUrl(wompiUrl).build();
        int amountCents  = payment.getAmount().multiply(BigDecimal.valueOf(100)).intValue();
        try {
            var result = client.post()
                    .uri("/payment_links")
                    .header("Authorization", "Bearer " + wompiPrivateKey)
                    .bodyValue(Map.of(
                            "name",             "GoServi - Pago de servicio",
                            "description",      "Reserva #" + payment.getBookingId(),
                            "single_use",       true,
                            "collect_shipping", false,
                            "amount_in_cents",  amountCents,
                            "currency",         "COP",
                            "reference",        payment.getId()
                    ))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (result != null && result.containsKey("data")) {
                var data = (Map<?, ?>) result.get("data");
                return "https://checkout.wompi.co/l/" + data.get("id");
            }
        } catch (Exception e) {
            log.warn("Wompi link generation failed: {}", e.getMessage());
        }
        return null;
    }

    private PaymentDtos.PaymentResponse toResponse(Payment p) {
        return PaymentDtos.PaymentResponse.builder()
                .id(p.getId())
                .bookingId(p.getBookingId())
                .amount(p.getAmount())
                .platformFee(p.getPlatformFee())
                .professionalAmount(p.getProfessionalAmount())
                .status(p.getStatus())
                .paymentLink(p.getPaymentLink())
                .createdAt(p.getCreatedAt())
                .paidAt(p.getPaidAt())
                .build();
    }

    private PaymentDtos.WithdrawalResponse toWithdrawalResponse(
            com.goservi.payment.entity.WithdrawalRequest w) {
        return PaymentDtos.WithdrawalResponse.builder()
                .id(w.getId())
                .amount(w.getAmount())
                .bankName(w.getBankName())
                .accountNumber(w.getAccountNumber())
                .accountHolder(w.getAccountHolder())
                .status(w.getStatus())
                .createdAt(w.getCreatedAt())
                .build();
    }

    private String safeGetName(Long userId) {
        if (userId == null) return null;
        try {
            return userProfileService.getSummary(userId).getFullName();
        } catch (Exception e) {
            return "Profesional #" + userId;
        }
    }
}