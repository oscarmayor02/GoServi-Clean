package com.goservi.booking.service.impl;

import com.goservi.booking.dto.BookingDtos;
import com.goservi.booking.entity.Booking;
import com.goservi.booking.entity.BookingStatus;
import com.goservi.booking.repository.BookingRepository;
import com.goservi.booking.service.BookingService;
import com.goservi.chat.service.ChatService;
import com.goservi.common.exception.BadRequestException;
import com.goservi.common.exception.NotFoundException;
import com.goservi.common.exception.UnauthorizedException;
import com.goservi.notification.service.NotificationService;
import com.goservi.common.dto.NotificationRequest;
import com.goservi.common.dto.NotificationType;
import com.goservi.serviceoffer.service.ServiceOfferService;
import com.goservi.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository repo;
    private final ServiceOfferService offerService;
    private final UserProfileService userProfileService;
    private final NotificationService notificationService;
    private final com.goservi.payment.repository.PaymentRepository paymentRepo;
    private final SimpMessagingTemplate ws;
    private final ChatService chatService;

    private static final java.math.BigDecimal PLATFORM_FEE = new java.math.BigDecimal("0.15");
    private static final java.math.BigDecimal PRO_SHARE    = new java.math.BigDecimal("0.85");

    @Override
    public BookingDtos.BookingResponse create(Long clientId, BookingDtos.BookingRequest req) {
        if (!offerService.existsById(req.getServiceOfferId()))
            throw new NotFoundException("Servicio no encontrado");

        var offer = offerService.getById(req.getServiceOfferId());

        boolean hasUnpaid = repo.findByClientIdAndStatus(clientId, BookingStatus.COMPLETED)
                .stream().anyMatch(b -> {
                    try {
                        var payment = paymentRepo.findByBookingId(b.getId());
                        return payment.isEmpty() || payment.get().getStatus() ==
                                com.goservi.payment.entity.PaymentStatus.PENDING;
                    } catch (Exception e) {
                        return false;
                    }
                });
        if (hasUnpaid)
            throw new BadRequestException(
                    "Tienes un servicio completado pendiente de pago. Paga primero para continuar.");

        var start = req.getStartLocal();
        var end = req.getEndLocal();
        if (!end.isAfter(start)) throw new BadRequestException("Intervalo de tiempo inválido");

        long dur = java.time.Duration.between(start, end).toMinutes();
        if (dur < offer.getMinDurationMin())
            throw new BadRequestException("Duración mínima: " + offer.getMinDurationMin() + " min");
        if (dur > offer.getMaxDurationMin())
            throw new BadRequestException("Duración máxima: " + offer.getMaxDurationMin() + " min");

        var nowPlusLead = LocalDateTime.now().plusMinutes(offer.getLeadTimeMin());
        if (start.isBefore(nowPlusLead)) throw new BadRequestException("Reserva con muy poco tiempo de anticipación");

        if (repo.existsOverlap(req.getServiceOfferId(), start, end))
            throw new BadRequestException("Ese horario ya está ocupado");

        int used = repo.countByServiceOfferIdAndDate(req.getServiceOfferId(), start.toLocalDate());
        if (used >= offer.getDailyCapacity())
            throw new BadRequestException("Capacidad diaria del profesional alcanzada");

        String code = RandomStringUtils.randomNumeric(6);
        var status = offer.isInstantBookingEnabled() ? BookingStatus.CONFIRMED : BookingStatus.PENDING;

        Booking booking = Booking.builder()
                .serviceOfferId(req.getServiceOfferId())
                .clientId(clientId)
                .professionalId(req.getProfessionalId())
                .startLocal(start)
                .endLocal(end)
                .status(status)
                .verificationCode(code)
                .clientLat(req.getClientLat())
                .clientLng(req.getClientLng())
                .clientAddress(req.getClientAddress())
                .build();

        var saved = repo.save(booking);
        notifyBookingCreated(saved, status);
        return toResponse(saved);
    }

    @Override
    public BookingDtos.BookingResponse accept(String id, Long professionalId) {
        Booking b = getAndValidateProfessional(id, professionalId);
        if (b.getStatus() != BookingStatus.PENDING)
            throw new BadRequestException("Solo se pueden aceptar reservas PENDING");
        b.setStatus(BookingStatus.CONFIRMED);
        var saved = repo.save(b);
        notifyConfirmed(saved);
        return toResponse(saved);
    }

    @Override
    public BookingDtos.BookingResponse reject(String id, Long professionalId) {
        Booking b = getAndValidateProfessional(id, professionalId);
        if (b.getStatus() != BookingStatus.PENDING)
            throw new BadRequestException("Solo se pueden rechazar reservas PENDING");
        b.setStatus(BookingStatus.REJECTED);
        return toResponse(repo.save(b));
    }

    @Override
    public BookingDtos.BookingResponse arrive(String id, Long professionalId) {
        Booking b = getAndValidateProfessional(id, professionalId);
        if (b.getStatus() != BookingStatus.CONFIRMED)
            throw new BadRequestException("Solo puedes notificar llegada en reservas CONFIRMED");

        b.setStatus(BookingStatus.ARRIVED);
        var saved = repo.save(b);

        try {
            var profSummary = userProfileService.getSummary(professionalId);
            ws.convertAndSend("/topic/booking/" + b.getClientId() + "/events", Map.of(
                    "event", "PROFESSIONAL_ARRIVED",
                    "bookingId", b.getId(),
                    "professionalName", profSummary.getFullName(),
                    "message", profSummary.getFullName() + " llegó. Dále tu código."
            ));
        } catch (Exception e) {
            log.warn("WS arrival notification failed: {}", e.getMessage());
        }

        try {
            var clientSummary = userProfileService.getSummary(b.getClientId());
            notificationService.send(NotificationRequest.builder()
                    .to(clientSummary.getEmail())
                    .type(NotificationType.EMAIL)
                    .subject("¡Tu profesional llegó!")
                    .message("Tu profesional llegó. Código: " + b.getVerificationCode())
                    .metadata(Map.of("bookingId", b.getId(), "event", "PROFESSIONAL_ARRIVED"))
                    .build());
        } catch (Exception e) {
            log.warn("Email arrival notification failed: {}", e.getMessage());
        }

        return toResponse(saved);
    }

    @Override
    public BookingDtos.BookingResponse verify(String id, BookingDtos.VerifyRequest req, Long professionalId) {
        Booking b = getAndValidateProfessional(id, professionalId);
        if (b.getStatus() != BookingStatus.ARRIVED && b.getStatus() != BookingStatus.CONFIRMED)
            throw new BadRequestException("La reserva debe estar ARRIVED o CONFIRMED para verificar");
        if (!b.getVerificationCode().equals(req.getCode()))
            throw new BadRequestException("Código de seguridad inválido");
        b.setStatus(BookingStatus.IN_PROGRESS);
        b.setVerifiedAt(LocalDateTime.now());
        return toResponse(repo.save(b));
    }

    @Override
    public BookingDtos.BookingResponse complete(String id, Long professionalId) {
        Booking b = getAndValidateProfessional(id, professionalId);
        if (b.getStatus() != BookingStatus.IN_PROGRESS)
            throw new BadRequestException("La reserva debe estar IN_PROGRESS para completar");
        b.setStatus(BookingStatus.COMPLETED);
        b.setCompletedAt(LocalDateTime.now());

        try {
            var clientSummary = userProfileService.getSummary(b.getClientId());
            ws.convertAndSend("/topic/booking/" + b.getClientId() + "/events", Map.of(
                    "event", "SERVICE_COMPLETED", "bookingId", b.getId(),
                    "message", "El servicio ha sido completado."));
            notificationService.send(NotificationRequest.builder()
                    .to(clientSummary.getEmail())
                    .type(NotificationType.EMAIL)
                    .subject("Servicio completado")
                    .message("Tu servicio ha sido marcado como completado.")
                    .metadata(Map.of("bookingId", b.getId(), "event", "SERVICE_COMPLETED"))
                    .build());
        } catch (Exception e) {
            log.warn("Complete notification failed: {}", e.getMessage());
        }

        return toResponse(repo.save(b));
    }

    @Override
    public BookingDtos.BookingResponse cancel(String id, Long userId) {
        Booking b = repo.findById(id).orElseThrow(() -> new NotFoundException("Reserva no encontrada"));
        if (!b.getClientId().equals(userId) && !b.getProfessionalId().equals(userId))
            throw new UnauthorizedException("Sin permiso");
        if (b.getStatus() == BookingStatus.COMPLETED || b.getStatus() == BookingStatus.PAID)
            throw new BadRequestException("No se puede cancelar una reserva completada o pagada");
        b.setStatus(BookingStatus.CANCELLED);
        b.setCancelledAt(LocalDateTime.now());
        return toResponse(repo.save(b));
    }

    /** Llamado por el profesional desde la app */
    @Override
    public BookingDtos.BookingResponse markPaid(String id, Long professionalId) {
        Booking b = getAndValidateProfessional(id, professionalId);
        if (b.getStatus() != BookingStatus.COMPLETED)
            throw new BadRequestException("Solo se puede marcar como pagada una reserva COMPLETED");
        return doPaid(b);
    }

    /** Llamado internamente por el webhook de Wompi — sin validación de profesional */
    @Override
    public BookingDtos.BookingResponse markPaidInternal(String id) {
        Booking b = repo.findById(id).orElseThrow(() -> new NotFoundException("Reserva no encontrada"));
        if (b.getStatus() == BookingStatus.PAID) return toResponse(b); // idempotente
        return doPaid(b);
    }

    /** Lógica común de marcar como pagado */
    private BookingDtos.BookingResponse doPaid(Booking b) {
        b.setStatus(BookingStatus.PAID);

        // Notificaciones al cliente
        try {
            var clientSummary = userProfileService.getSummary(b.getClientId());
            ws.convertAndSend("/topic/booking/" + b.getClientId() + "/events", Map.of(
                    "event", "BOOKING_PAID", "bookingId", b.getId(),
                    "message", "Pago confirmado. ¡Gracias por usar GoServi!"));
            notificationService.send(NotificationRequest.builder()
                    .to(clientSummary.getEmail())
                    .type(NotificationType.EMAIL)
                    .subject("Pago confirmado")
                    .message("Tu pago ha sido confirmado. ¡Gracias por usar GoServi!")
                    .metadata(Map.of("bookingId", b.getId(), "event", "BOOKING_PAID"))
                    .build());
        } catch (Exception e) {
            log.warn("Paid notification failed: {}", e.getMessage());
        }

        // ✅ Cerrar el thread de chat asociado al booking
        try {
            chatService.closeThreadByBookingId(b.getId());
        } catch (Exception e) {
            log.warn("No se pudo cerrar el thread del booking {}: {}", b.getId(), e.getMessage());
        }

        return toResponse(repo.save(b));
    }

    @Override
    public BookingDtos.BookingResponse getById(String id) {
        return repo.findById(id).map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Reserva no encontrada"));
    }

    @Override
    public List<BookingDtos.BookingResponse> getByClient(Long clientId) {
        return repo.findByClientId(clientId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<BookingDtos.BookingResponse> getByProfessional(Long professionalId) {
        return repo.findByProfessionalId(professionalId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── PRIVATE ──────────────────────────────────────────────────────────────

    private Booking getAndValidateProfessional(String id, Long professionalId) {
        Booking b = repo.findById(id).orElseThrow(() -> new NotFoundException("Reserva no encontrada"));
        if (!b.getProfessionalId().equals(professionalId))
            throw new UnauthorizedException("Solo el profesional asignado puede realizar esta acción");
        return b;
    }

    private void notifyBookingCreated(Booking b, BookingStatus status) {
        try {
            var clientSummary = userProfileService.getSummary(b.getClientId());
            var profSummary = userProfileService.getSummary(b.getProfessionalId());
            if (status == BookingStatus.CONFIRMED) {
                notifyConfirmed(b);
            } else {
                notificationService.send(NotificationRequest.builder()
                        .to(profSummary.getEmail())
                        .type(NotificationType.EMAIL)
                        .subject("Nueva solicitud de servicio")
                        .message("Nueva solicitud de " + clientSummary.getFullName() + ". Acepta o rechaza desde la app.")
                        .metadata(Map.of("bookingId", b.getId(), "event", "BOOKING_REQUEST"))
                        .build());
            }
        } catch (Exception e) {
            log.warn("Could not send booking notification: {}", e.getMessage());
        }
    }

    private void notifyConfirmed(Booking b) {
        try {
            var clientSummary = userProfileService.getSummary(b.getClientId());
            var profSummary = userProfileService.getSummary(b.getProfessionalId());
            notificationService.send(NotificationRequest.builder()
                    .to(clientSummary.getEmail())
                    .type(NotificationType.EMAIL)
                    .subject("Reserva confirmada — Código de seguridad")
                    .message("Reserva confirmada. Código: " + b.getVerificationCode() +
                            ". Compártelo SOLO con el profesional cuando llegue.")
                    .metadata(Map.of("bookingId", b.getId(), "event", "BOOKING_CONFIRMED"))
                    .build());
            notificationService.send(NotificationRequest.builder()
                    .to(profSummary.getEmail())
                    .type(NotificationType.EMAIL)
                    .subject("Reserva confirmada")
                    .message("Servicio con " + clientSummary.getFullName() + " confirmado. Código: " + b.getVerificationCode())
                    .metadata(Map.of("bookingId", b.getId(), "event", "BOOKING_CONFIRMED"))
                    .build());
        } catch (Exception e) {
            log.warn("Could not send confirmation notifications: {}", e.getMessage());
        }
    }

    private BookingDtos.BookingResponse toResponse(Booking b) {
        String clientName = null;
        String professionalName = null;
        String serviceTitle = null;
        String clientPhoto = null;
        String professionalPhoto = null;
        java.math.BigDecimal pricePerHour = null;
        Integer discountPercent = 0;

        try {
            var cs = userProfileService.getSummary(b.getClientId());
            clientName = cs.getFullName();
            clientPhoto = cs.getPhotoUrl();
        } catch (Exception e) {
            log.warn("client summary {}: {}", b.getClientId(), e.getMessage());
        }

        try {
            var ps = userProfileService.getSummary(b.getProfessionalId());
            professionalName = ps.getFullName();
            professionalPhoto = ps.getPhotoUrl();
        } catch (Exception e) {
            log.warn("prof summary {}: {}", b.getProfessionalId(), e.getMessage());
        }

        try {
            var offer = offerService.getById(b.getServiceOfferId());
            serviceTitle = offer.getTitle();
            pricePerHour = offer.getPricePerHour();
            discountPercent = offer.getDiscountPercent() != null ? offer.getDiscountPercent() : 0;
        } catch (Exception e) {
            log.warn("offer {}: {}", b.getServiceOfferId(), e.getMessage());
        }

        Integer durationMinutes = null;
        java.math.BigDecimal totalPrice = null;
        java.math.BigDecimal platformFee = null;
        java.math.BigDecimal professionalAmount = null;

        if (b.getStartLocal() != null && b.getEndLocal() != null && pricePerHour != null) {
            long mins = java.time.Duration.between(b.getStartLocal(), b.getEndLocal()).toMinutes();
            durationMinutes = (int) mins;

            java.math.BigDecimal hours = java.math.BigDecimal.valueOf(mins)
                    .divide(java.math.BigDecimal.valueOf(60), 4, java.math.RoundingMode.HALF_UP);
            java.math.BigDecimal gross = pricePerHour.multiply(hours)
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            if (discountPercent > 0) {
                java.math.BigDecimal factor = java.math.BigDecimal.ONE
                        .subtract(java.math.BigDecimal.valueOf(discountPercent)
                                .divide(java.math.BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP));
                gross = gross.multiply(factor).setScale(2, java.math.RoundingMode.HALF_UP);
            }

            totalPrice = gross;
            platformFee = gross.multiply(PLATFORM_FEE).setScale(2, java.math.RoundingMode.HALF_UP);
            professionalAmount = gross.multiply(PRO_SHARE).setScale(2, java.math.RoundingMode.HALF_UP);
        }

        return BookingDtos.BookingResponse.builder()
                .id(b.getId())
                .serviceOfferId(b.getServiceOfferId())
                .clientId(b.getClientId())
                .professionalId(b.getProfessionalId())
                .startLocal(b.getStartLocal())
                .endLocal(b.getEndLocal())
                .status(b.getStatus())
                .verificationCode(b.getVerificationCode())
                .verifiedAt(b.getVerifiedAt())
                .clientLat(b.getClientLat())
                .clientLng(b.getClientLng())
                .clientAddress(b.getClientAddress())
                .createdAt(b.getCreatedAt())
                .clientName(clientName)
                .clientPhoto(clientPhoto)
                .professionalName(professionalName)
                .professionalPhoto(professionalPhoto)
                .serviceTitle(serviceTitle)
                .pricePerHour(pricePerHour)
                .durationMinutes(durationMinutes)
                .totalPrice(totalPrice)
                .platformFee(platformFee)
                .professionalAmount(professionalAmount)
                .build();
    }
}