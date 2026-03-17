package com.goservi.payment.service.impl;

import com.goservi.auth.repository.AuthUserRepository;
import com.goservi.booking.service.BookingService;
import com.goservi.common.exception.BadRequestException;
import com.goservi.common.exception.NotFoundException;
import com.goservi.notification.service.NotificationService;
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
    private final NotificationService notificationService;
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
            // Si ya tiene un payment pendiente (Wompi con link o CASH), retornarlo
            if (p.getStatus() == PaymentStatus.PENDING && p.getPaymentLink() != null)
                return toResponse(p);
            if (p.getStatus() == PaymentStatus.PENDING_CASH)
                return toResponse(p);
        }

        // Determinar método de pago
        PaymentMethod method = PaymentMethod.WOMPI;
        if (req.getPaymentMethod() != null && req.getPaymentMethod().equalsIgnoreCase("CASH")) {
            method = PaymentMethod.CASH;
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
                .paymentMethod(method)
                .status(method == PaymentMethod.CASH ? PaymentStatus.PENDING_CASH : PaymentStatus.PENDING)
                .build();
        payment = paymentRepo.save(payment);

        if (method == PaymentMethod.WOMPI) {
            try {
                payment.setPaymentLink(generateWompiLink(payment));
            } catch (Exception e) {
                log.warn("Wompi link generation failed: {}", e.getMessage());
            }
        } else {
            // CASH: NO marcar booking como PAID todavía
            // Solo notificar al profesional y al admin
            notifyCashPaymentToProfessional(payment);
            notifyCashPaymentToAdmin(payment);
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
                .paymentMethod(p.getPaymentMethod())
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

    @Override
    public PaymentDtos.PaymentResponse adminMarkPaid(String paymentId) {
        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Pago no encontrado"));
        if (payment.getStatus() == PaymentStatus.PAID)
            throw new BadRequestException("Este pago ya está marcado como pagado");

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepo.save(payment);

        // Acreditar al profesional
        creditProfessional(payment);

        // Marcar booking como pagado
        try {
            bookingService.markPaidInternal(payment.getBookingId());
        } catch (Exception e) {
            log.warn("Could not mark booking as paid: {}", e.getMessage());
        }

        log.info("Admin marked payment {} as PAID (method={})", paymentId, payment.getPaymentMethod());
        return toResponse(payment);
    }
    private void notifyCashPayment(Payment payment) {
        try {
            var profSummary = userProfileService.getSummary(payment.getProfessionalId());

            String message = String.format(
                    "El cliente pagó en efectivo $%s por la reserva #%s. " +
                            "Comisión GoServi: $%s. " +
                            "Transfiere la comisión a: Nequi 300XXXXXXX o Bancolombia 000-000000-00. " +
                            "No podrás aceptar nuevos servicios hasta que la comisión sea verificada.",
                    payment.getAmount().toPlainString(),
                    payment.getBookingId(),
                    payment.getPlatformFee().toPlainString()
            );

            notificationService.send(com.goservi.common.dto.NotificationRequest.builder()
                    .to(profSummary.getEmail())
                    .type(com.goservi.common.dto.NotificationType.EMAIL)
                    .subject("Pago en efectivo — Transfiere tu comisión GoServi")
                    .message(message)
                    .metadata(java.util.Map.of(
                            "bookingId", payment.getBookingId(),
                            "event", "CASH_PAYMENT",
                            "platformFee", payment.getPlatformFee().toPlainString()
                    ))
                    .build());
        } catch (Exception e) {
            log.warn("Cash payment notification failed: {}", e.getMessage());
        }
    }

    @Override
    public PaymentDtos.PaymentResponse approveCashPayment(String paymentId) {
        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Pago no encontrado"));

        if (payment.getStatus() == PaymentStatus.PAID)
            throw new BadRequestException("Este pago ya fue aprobado");
        if (payment.getPaymentMethod() != PaymentMethod.CASH)
            throw new BadRequestException("Este pago no es en efectivo");

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepo.save(payment);

        // Ahora sí: acreditar al profesional
        creditProfessional(payment);

        // Ahora sí: marcar booking como PAID
        try {
            bookingService.markPaidInternal(payment.getBookingId());
        } catch (Exception e) {
            log.warn("Could not mark booking as paid: {}", e.getMessage());
        }

        // Notificar al profesional que fue aprobado
        try {
            var profSummary = userProfileService.getSummary(payment.getProfessionalId());
            notificationService.send(com.goservi.common.dto.NotificationRequest.builder()
                    .to(profSummary.getEmail())
                    .type(com.goservi.common.dto.NotificationType.EMAIL)
                    .subject("✅ Comisión verificada — ya puedes aceptar servicios")
                    .message("Tu comisión de $" + payment.getPlatformFee().toPlainString() +
                            " fue verificada. Ya puedes aceptar nuevos servicios.")
                    .metadata(java.util.Map.of("event", "CASH_APPROVED", "paymentId", paymentId))
                    .build());
        } catch (Exception e) {
            log.warn("Notification failed: {}", e.getMessage());
        }

        log.info("Admin approved cash payment {} — booking {} — fee {}",
                paymentId, payment.getBookingId(), payment.getPlatformFee());
        return toResponse(payment);
    }

    @Override
    public PaymentDtos.PaymentResponse rejectCashPayment(String paymentId, String reason) {
        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Pago no encontrado"));

        if (payment.getPaymentMethod() != PaymentMethod.CASH)
            throw new BadRequestException("Este pago no es en efectivo");

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepo.save(payment);

        // Notificar al profesional
        try {
            var profSummary = userProfileService.getSummary(payment.getProfessionalId());
            notificationService.send(com.goservi.common.dto.NotificationRequest.builder()
                    .to(profSummary.getEmail())
                    .type(com.goservi.common.dto.NotificationType.EMAIL)
                    .subject("⚠️ Comisión no verificada")
                    .message("No pudimos verificar tu transferencia de comisión ($" +
                            payment.getPlatformFee().toPlainString() + "). Motivo: " + reason +
                            ". Contacta soporte si crees que es un error.")
                    .metadata(java.util.Map.of("event", "CASH_REJECTED", "paymentId", paymentId))
                    .build());
        } catch (Exception e) {
            log.warn("Notification failed: {}", e.getMessage());
        }

        log.info("Admin rejected cash payment {} — reason: {}", paymentId, reason);
        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentDtos.CashPaymentAdminView> getPendingCashPayments() {
        return paymentRepo.findByPaymentMethodAndStatus(PaymentMethod.CASH, PaymentStatus.PENDING_CASH)
                .stream()
                .map(p -> {
                    String clientName = safeGetName(p.getClientId());
                    String profName = safeGetName(p.getProfessionalId());
                    String profEmail = null;
                    try {
                        profEmail = userProfileService.getSummary(p.getProfessionalId()).getEmail();
                    } catch (Exception ignored) {}

                    return PaymentDtos.CashPaymentAdminView.builder()
                            .paymentId(p.getId())
                            .bookingId(p.getBookingId())
                            .clientId(p.getClientId())
                            .clientName(clientName)
                            .professionalId(p.getProfessionalId())
                            .professionalName(profName)
                            .professionalEmail(profEmail)
                            .totalAmount(p.getAmount())
                            .platformFee(p.getPlatformFee())
                            .professionalAmount(p.getProfessionalAmount())
                            .status(p.getStatus().name())
                            .createdAt(p.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private void notifyCashPaymentToProfessional(Payment payment) {
        try {
            var profSummary = userProfileService.getSummary(payment.getProfessionalId());
            String message = String.format(
                    "El cliente pagó en efectivo $%s por la reserva #%s.\n\n" +
                            "💰 Comisión GoServi que debes transferir: $%s\n\n" +
                            "📱 Transfiere a:\n" +
                            "• Nequi: 300XXXXXXX\n" +
                            "• Bancolombia: 000-000000-00\n\n" +
                            "⚠️ No podrás aceptar nuevos servicios hasta que verifiquemos tu transferencia.",
                    payment.getAmount().toPlainString(),
                    payment.getBookingId(),
                    payment.getPlatformFee().toPlainString()
            );
            notificationService.send(com.goservi.common.dto.NotificationRequest.builder()
                    .to(profSummary.getEmail())
                    .type(com.goservi.common.dto.NotificationType.EMAIL)
                    .subject("💵 Pago en efectivo — Transfiere tu comisión GoServi ($" +
                            payment.getPlatformFee().toPlainString() + ")")
                    .message(message)
                    .metadata(java.util.Map.of(
                            "bookingId", payment.getBookingId(),
                            "event", "CASH_PAYMENT_PENDING",
                            "platformFee", payment.getPlatformFee().toPlainString()
                    ))
                    .build());
        } catch (Exception e) {
            log.warn("Cash notification to professional failed: {}", e.getMessage());
        }
    }

    private void notifyCashPaymentToAdmin(Payment payment) {
        try {
            // Buscar admins
            var admins = authRepo.findAll().stream()
                    .filter(u -> u.getRoles().contains(com.goservi.auth.entity.Role.ADMIN))
                    .collect(java.util.stream.Collectors.toList());

            String profName = safeGetName(payment.getProfessionalId());
            String clientName = safeGetName(payment.getClientId());

            for (var admin : admins) {
                notificationService.send(com.goservi.common.dto.NotificationRequest.builder()
                        .to(admin.getEmail())
                        .type(com.goservi.common.dto.NotificationType.EMAIL)
                        .subject("🔔 Nuevo pago en efectivo pendiente de comisión")
                        .message(String.format(
                                "Pago en efectivo registrado:\n" +
                                        "• Cliente: %s\n" +
                                        "• Profesional: %s\n" +
                                        "• Monto total: $%s\n" +
                                        "• Comisión esperada: $%s\n" +
                                        "• Booking: %s\n\n" +
                                        "Verifica la transferencia y aprueba desde el panel admin.",
                                clientName, profName,
                                payment.getAmount().toPlainString(),
                                payment.getPlatformFee().toPlainString(),
                                payment.getBookingId()
                        ))
                        .metadata(java.util.Map.of(
                                "event", "ADMIN_CASH_PENDING",
                                "paymentId", payment.getId()
                        ))
                        .build());
            }
        } catch (Exception e) {
            log.warn("Admin cash notification failed: {}", e.getMessage());
        }
    }
}