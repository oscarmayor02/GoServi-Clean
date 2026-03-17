package com.goservi.admin.service;

import com.goservi.admin.dto.AdminDtos;
import com.goservi.auth.entity.AuthUser;
import com.goservi.auth.entity.Role;
import com.goservi.auth.repository.AuthUserRepository;
import com.goservi.booking.entity.Booking;
import com.goservi.booking.entity.BookingStatus;
import com.goservi.booking.repository.BookingRepository;
import com.goservi.common.exception.NotFoundException;
import com.goservi.payment.entity.PaymentStatus;
import com.goservi.payment.entity.WithdrawalStatus;
import com.goservi.payment.repository.PaymentRepository;
import com.goservi.payment.repository.ProfessionalEarningRepository;
import com.goservi.payment.repository.WithdrawalRequestRepository;
import com.goservi.review.repository.ReviewRepository;
import com.goservi.serviceoffer.repository.ServiceOfferRepository;
import com.goservi.support.entity.SupportTicket;
import com.goservi.support.repository.SupportTicketRepository;
import com.goservi.user.repository.UserProfileRepository;
import com.goservi.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final AuthUserRepository authRepo;
    private final UserProfileRepository profileRepo;
    private final BookingRepository bookingRepo;
    private final PaymentRepository paymentRepo;
    private final ServiceOfferRepository offerRepo;
    private final ReviewRepository reviewRepo;
    private final UserProfileService userProfileService;
    private final PasswordEncoder passwordEncoder;
    private final WithdrawalRequestRepository withdrawalRepo;
    private final ProfessionalEarningRepository earningsRepo;
    private final SupportTicketRepository ticketRepo;

    // ══════════════════════════════════════════════════════════
    // DASHBOARD (sin cambios)
    // ══════════════════════════════════════════════════════════

    public AdminDtos.DashboardStats getDashboard() {
        var allUsers    = authRepo.findAll();
        var allBookings = bookingRepo.findAll();
        var allPayments = paymentRepo.findAll();
        var allOffers   = offerRepo.findAll();

        long totalClients = allUsers.stream()
                .filter(u -> u.getRoles().contains(Role.CLIENT)).count();
        long totalProfessionals = allUsers.stream()
                .filter(u -> u.getRoles().contains(Role.PROFESSIONAL)).count();
        long newUsers = allUsers.stream()
                .filter(u -> u.getCreatedAt() != null &&
                        u.getCreatedAt().isAfter(LocalDateTime.now().minusDays(30))).count();
        long pendingCash = paymentRepo.findByPaymentMethodAndStatus(
                com.goservi.payment.entity.PaymentMethod.CASH,
                com.goservi.payment.entity.PaymentStatus.PENDING_CASH).size();

        BigDecimal totalRevenue = allPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal platformRevenue = allPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .map(p -> p.getPlatformFee() != null ? p.getPlatformFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDateTime startOfWeek  = LocalDateTime.now().minusDays(7);
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);

        BigDecimal revenueThisWeek = allPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID
                        && p.getPaidAt() != null && p.getPaidAt().isAfter(startOfWeek))
                .map(p -> p.getPlatformFee() != null ? p.getPlatformFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal revenueThisMonth = allPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID
                        && p.getPaidAt() != null && p.getPaidAt().isAfter(startOfMonth))
                .map(p -> p.getPlatformFee() != null ? p.getPlatformFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var pendingWithdrawals = withdrawalRepo.findByStatus(WithdrawalStatus.PENDING);
        BigDecimal pendingWithdrawalsAmount = pendingWithdrawals.stream()
                .map(w -> w.getAmount() != null ? w.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long openTickets = ticketRepo.countByStatus("OPEN");
        Double avgRating = reviewRepo.getAverageRating(0L);

        return AdminDtos.DashboardStats.builder()
                .totalUsers(allUsers.size())
                .totalClients(totalClients)
                .totalProfessionals(totalProfessionals)
                .newUsersLast30Days(newUsers)
                .totalServiceOffers(allOffers.size())
                .activeServiceOffers(allOffers.stream().filter(o -> o.isActive()).count())
                .totalBookings(allBookings.size())
                .pendingBookings(countByStatus(allBookings, BookingStatus.PENDING))
                .confirmedBookings(countByStatus(allBookings, BookingStatus.CONFIRMED))
                .inProgressBookings(countByStatus(allBookings, BookingStatus.IN_PROGRESS))
                .completedBookings(countByStatus(allBookings, BookingStatus.COMPLETED))
                .cancelledBookings(countByStatus(allBookings, BookingStatus.CANCELLED))
                .paidBookings(countByStatus(allBookings, BookingStatus.PAID))
                .totalRevenue(totalRevenue)
                .platformRevenue(platformRevenue)
                .revenueThisWeek(revenueThisWeek)
                .revenueThisMonth(revenueThisMonth)
                .totalPayments(allPayments.size())
                .pendingPayments(allPayments.stream()
                        .filter(p -> p.getStatus() == PaymentStatus.PENDING).count())
                .pendingWithdrawals((long) pendingWithdrawals.size())
                .pendingWithdrawalsAmount(pendingWithdrawalsAmount)
                .openTickets(openTickets)
                .totalReviews(reviewRepo.count())
                .averageRating(avgRating)
                .pendingCashPayments(pendingCash)
                .build();
    }

    public List<AdminDtos.RevenuePoint> getRevenueChart(int days) {
        LocalDateTime from = LocalDateTime.now().minusDays(days);
        var payments = paymentRepo.findAll().stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID
                        && p.getPaidAt() != null && p.getPaidAt().isAfter(from))
                .collect(Collectors.toList());

        var fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        var grouped = payments.stream()
                .collect(Collectors.groupingBy(p -> p.getPaidAt().format(fmt)));

        List<AdminDtos.RevenuePoint> result = new ArrayList<>();
        for (int i = days; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).format(fmt);
            var dayPays = grouped.getOrDefault(date, List.of());
            BigDecimal amount = dayPays.stream()
                    .map(p -> p.getPlatformFee() != null ? p.getPlatformFee() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.add(AdminDtos.RevenuePoint.builder()
                    .date(date).amount(amount).bookingCount(dayPays.size()).build());
        }
        return result;
    }

    public List<AdminDtos.TopProfessional> getTopProfessionals(int limit) {
        return bookingRepo.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.PAID || b.getStatus() == BookingStatus.COMPLETED)
                .collect(Collectors.groupingBy(Booking::getProfessionalId, Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(e -> {
                    Long profId = e.getKey();
                    Double rating = null;
                    try { rating = reviewRepo.getAverageRating(profId); } catch (Exception ignored) {}
                    BigDecimal earned = earningsRepo.findByProfessionalId(profId)
                            .map(pe -> pe.getTotalEarned()).orElse(BigDecimal.ZERO);
                    return AdminDtos.TopProfessional.builder()
                            .id(profId)
                            .name(safeGetName(profId))
                            .completedBookings(e.getValue())
                            .totalEarned(earned)
                            .avgRating(rating != null ? rating : 0.0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════
    // USUARIOS (sin cambios)
    // ══════════════════════════════════════════════════════════

    public List<AdminDtos.UserAdminView> getAllUsers(String role, Boolean active) {
        return authRepo.findAll().stream()
                .filter(u -> role == null || u.getRoles().stream()
                        .anyMatch(r -> r.name().equalsIgnoreCase(role)))
                .filter(u -> active == null || u.isActive() == active)
                .map(this::toUserView)
                .collect(Collectors.toList());
    }

    public List<AdminDtos.UserAdminView> getAllUsers() {
        return getAllUsers(null, null);
    }

    public AdminDtos.UserAdminView getUserById(Long id) {
        return authRepo.findById(id)
                .map(this::toUserView)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    @Transactional
    public void toggleUserActive(Long id) {
        var user = authRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        user.setActive(!user.isActive());
        authRepo.save(user);
    }

    // ══════════════════════════════════════════════════════════
    // RESERVAS (sin cambios)
    // ══════════════════════════════════════════════════════════

    public List<AdminDtos.BookingAdminView> getAllBookings(String status, LocalDate from, LocalDate to) {
        return bookingRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(b -> status == null || b.getStatus().name().equalsIgnoreCase(status))
                .filter(b -> from == null || (b.getCreatedAt() != null
                        && !b.getCreatedAt().toLocalDate().isBefore(from)))
                .filter(b -> to == null || (b.getCreatedAt() != null
                        && !b.getCreatedAt().toLocalDate().isAfter(to)))
                .map(b -> {
                    BigDecimal amount = null;
                    String payStatus = null;
                    try {
                        var payment = paymentRepo.findByBookingId(b.getId());
                        if (payment.isPresent()) {
                            amount    = payment.get().getAmount();
                            payStatus = payment.get().getStatus().name();
                        }
                    } catch (Exception ignored) {}

                    return AdminDtos.BookingAdminView.builder()
                            .id(b.getId())
                            .clientName(safeGetName(b.getClientId()))
                            .professionalName(safeGetName(b.getProfessionalId()))
                            .status(b.getStatus().name())
                            .scheduledAt(b.getStartLocal())
                            .createdAt(b.getCreatedAt())
                            .totalPrice(amount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<AdminDtos.BookingAdminView> getAllBookings() {
        return getAllBookings(null, null, null);
    }

    // ══════════════════════════════════════════════════════════
    // PAGOS (sin cambios)
    // ══════════════════════════════════════════════════════════

    public List<AdminDtos.PaymentAdminView> getAllPayments(String status) {
        return paymentRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(p -> status == null || p.getStatus().name().equalsIgnoreCase(status))
                .map(p -> AdminDtos.PaymentAdminView.builder()
                        .id(p.getId())
                        .bookingId(p.getBookingId())
                        .clientName(safeGetName(p.getClientId()))
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

    public List<AdminDtos.PaymentAdminView> getAllPayments() {
        return getAllPayments(null);
    }

    // ══════════════════════════════════════════════════════════
    // RETIROS (sin cambios)
    // ══════════════════════════════════════════════════════════

    public List<AdminDtos.WithdrawalAdminView> getAllWithdrawals(String status) {
        var list = status != null
                ? withdrawalRepo.findByStatus(WithdrawalStatus.valueOf(status.toUpperCase()))
                : withdrawalRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        return list.stream().map(w -> AdminDtos.WithdrawalAdminView.builder()
                        .id(w.getId())
                        .professionalId(w.getProfessionalId())
                        .professionalName(safeGetName(w.getProfessionalId()))
                        .professionalEmail(safeGetEmail(w.getProfessionalId()))
                        .amount(w.getAmount())
                        .bankAccount(w.getAccountNumber())
                        .bankName(w.getBankName())
                        .status(w.getStatus().name())
                        .rejectionReason(w.getAdminNote())
                        .createdAt(w.getCreatedAt())
                        .processedAt(w.getProcessedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void processWithdrawal(String id) {
        var w = withdrawalRepo.findById(String.valueOf(id))
                .orElseThrow(() -> new NotFoundException("Retiro no encontrado"));
        if (w.getStatus() == WithdrawalStatus.PROCESSED)
            throw new IllegalStateException("El retiro ya fue procesado");

        earningsRepo.findByProfessionalId(w.getProfessionalId()).ifPresent(e -> {
            e.setPendingWithdrawal(e.getPendingWithdrawal().subtract(w.getAmount()));
            e.setTotalWithdrawn(e.getTotalWithdrawn().add(w.getAmount()));
            earningsRepo.save(e);
        });
        w.setStatus(WithdrawalStatus.PROCESSED);
        w.setProcessedAt(LocalDateTime.now());
        withdrawalRepo.save(w);
        log.info("Retiro {} procesado — profesional {} — ${}", id, w.getProfessionalId(), w.getAmount());
    }

    @Transactional
    public void rejectWithdrawal(String id, String reason) {
        var w = withdrawalRepo.findById(String.valueOf(id))
                .orElseThrow(() -> new NotFoundException("Retiro no encontrado"));
        if (w.getStatus() == WithdrawalStatus.PROCESSED)
            throw new IllegalStateException("No se puede rechazar un retiro ya procesado");

        earningsRepo.findByProfessionalId(w.getProfessionalId()).ifPresent(e -> {
            e.setPendingWithdrawal(e.getPendingWithdrawal().subtract(w.getAmount()));
            e.setAvailableBalance(e.getAvailableBalance().add(w.getAmount()));
            earningsRepo.save(e);
        });
        w.setStatus(WithdrawalStatus.REJECTED);
        w.setAdminNote(reason);
        withdrawalRepo.save(w);
        log.info("Retiro {} rechazado — motivo: {}", id, reason);
    }

    // ══════════════════════════════════════════════════════════
    // TICKETS (sin cambios)
    // ══════════════════════════════════════════════════════════

    public List<AdminDtos.TicketAdminView> getAllTickets(String status, String type) {
        return ticketRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(t -> status == null || t.getStatus().equalsIgnoreCase(status))
                .filter(t -> type == null   || t.getType().equalsIgnoreCase(type))
                .map(this::toTicketView)
                .collect(Collectors.toList());
    }

    public AdminDtos.TicketAdminView getTicketById(Long id) {
        return ticketRepo.findById(id)
                .map(this::toTicketView)
                .orElseThrow(() -> new NotFoundException("Ticket no encontrado"));
    }

    @Transactional
    public void respondTicket(Long id, AdminDtos.TicketRespondRequest req, UserDetails admin) {
        var ticket = ticketRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Ticket no encontrado"));
        ticket.setAdminResponse(req.getResponse());
        ticket.setStatus(req.getStatus() != null ? req.getStatus() : "IN_REVIEW");
        ticket.setAdminId(getUserIdFromDetails(admin));
        ticketRepo.save(ticket);
    }

    // ══════════════════════════════════════════════════════════
    // REPORTES (sin cambios)
    // ══════════════════════════════════════════════════════════

    public AdminDtos.RevenueReport getRevenueReport(LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt   = to.plusDays(1).atStartOfDay();

        var payments = paymentRepo.findAll().stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID
                        && p.getPaidAt() != null
                        && p.getPaidAt().isAfter(fromDt)
                        && p.getPaidAt().isBefore(toDt))
                .collect(Collectors.toList());

        BigDecimal totalRevenue = payments.stream()
                .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal platformFees = payments.stream()
                .map(p -> p.getPlatformFee() != null ? p.getPlatformFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        var grouped = payments.stream()
                .collect(Collectors.groupingBy(p -> p.getPaidAt().format(fmt)));

        List<AdminDtos.RevenuePoint> daily = new ArrayList<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            String dateStr = cursor.format(fmt);
            var dayPays = grouped.getOrDefault(dateStr, List.of());
            BigDecimal dayAmt = dayPays.stream()
                    .map(p -> p.getPlatformFee() != null ? p.getPlatformFee() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            daily.add(AdminDtos.RevenuePoint.builder()
                    .date(dateStr).amount(dayAmt).bookingCount(dayPays.size()).build());
            cursor = cursor.plusDays(1);
        }

        return AdminDtos.RevenueReport.builder()
                .period(from + " / " + to)
                .totalRevenue(totalRevenue)
                .platformFees(platformFees)
                .totalBookings(payments.size())
                .totalPayments(payments.size())
                .dailyBreakdown(daily)
                .build();
    }

    // ══════════════════════════════════════════════════════════
    // SERVICIOS (sin cambios)
    // ══════════════════════════════════════════════════════════

    public List<AdminDtos.ServiceOfferAdminView> getAllOffers() {
        return offerRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(o -> AdminDtos.ServiceOfferAdminView.builder()
                        .id(o.getId())
                        .professionalName(safeGetName(o.getUserId()))
                        .title(o.getTitle())
                        .category(o.getCategory() != null ? o.getCategory().getName() : null)
                        .pricePerHour(o.getPricePerHour())
                        .active(o.isActive())
                        .createdAt(o.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void toggleOfferActive(Long id) {
        var offer = offerRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Servicio no encontrado"));
        offer.setActive(!offer.isActive());
        offerRepo.save(offer);
    }

    // ══════════════════════════════════════════════════════════
    // ★★★ NUEVOS — RANKING PROFESIONALES ★★★
    // ══════════════════════════════════════════════════════════

    public List<AdminDtos.ProfessionalRanking> getProfessionalRanking(int limit) {
        var allBookings = bookingRepo.findAll();
        var completedStatuses = Set.of(BookingStatus.COMPLETED, BookingStatus.PAID);

        // Agrupar bookings completados por profesional
        var byProfessional = allBookings.stream()
                .filter(b -> completedStatuses.contains(b.getStatus()))
                .collect(Collectors.groupingBy(Booking::getProfessionalId));

        return byProfessional.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().size(), a.getValue().size()))
                .limit(limit)
                .map(e -> {
                    Long profId = e.getKey();
                    List<Booking> profBookings = e.getValue();

                    long uniqueClients = profBookings.stream()
                            .map(Booking::getClientId).distinct().count();

                    BigDecimal earned = earningsRepo.findByProfessionalId(profId)
                            .map(pe -> pe.getTotalEarned()).orElse(BigDecimal.ZERO);

                    // Comisión generada para la plataforma
                    BigDecimal feeGenerated = paymentRepo.findAll().stream()
                            .filter(p -> profId.equals(p.getProfessionalId())
                                    && p.getStatus() == PaymentStatus.PAID
                                    && p.getPlatformFee() != null)
                            .map(p -> p.getPlatformFee())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    Double rating = null;
                    Long reviewCount = null;
                    try {
                        rating = reviewRepo.getAverageRating(profId);
                        reviewCount = reviewRepo.getReviewCount(profId);
                    } catch (Exception ignored) {}

                    long activeOffers = offerRepo.findAll().stream()
                            .filter(o -> profId.equals(o.getUserId()) && o.isActive()).count();

                    var user = authRepo.findById(profId).orElse(null);

                    return AdminDtos.ProfessionalRanking.builder()
                            .id(profId)
                            .name(safeGetName(profId))
                            .photo(safeGetPhoto(profId))
                            .email(safeGetEmail(profId))
                            .completedBookings(profBookings.size())
                            .uniqueClients(uniqueClients)
                            .totalEarned(earned)
                            .platformFeeGenerated(feeGenerated)
                            .avgRating(rating)
                            .reviewCount(reviewCount)
                            .activeOffers(activeOffers)
                            .memberSince(user != null ? user.getCreatedAt() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════
    // ★★★ NUEVOS — DETALLE PROFESIONAL ★★★
    // ══════════════════════════════════════════════════════════

    public AdminDtos.ProfessionalDetail getProfessionalDetail(Long profId) {
        var user = authRepo.findById(profId)
                .orElseThrow(() -> new NotFoundException("Profesional no encontrado"));
        var profile = profileRepo.findByAuthUserId(profId).orElse(null);

        var allBookings = bookingRepo.findByProfessionalId(profId);
        var completedStatuses = Set.of(BookingStatus.COMPLETED, BookingStatus.PAID);
        var completed = allBookings.stream()
                .filter(b -> completedStatuses.contains(b.getStatus())).collect(Collectors.toList());

        long cancelled = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();

        BigDecimal earned = earningsRepo.findByProfessionalId(profId)
                .map(pe -> pe.getTotalEarned()).orElse(BigDecimal.ZERO);
        BigDecimal balance = earningsRepo.findByProfessionalId(profId)
                .map(pe -> pe.getAvailableBalance()).orElse(BigDecimal.ZERO);

        Double rating = null;
        Long reviewCount = null;
        try {
            rating = reviewRepo.getAverageRating(profId);
            reviewCount = reviewRepo.getReviewCount(profId);
        } catch (Exception ignored) {}

        long activeOffers = offerRepo.findAll().stream()
                .filter(o -> profId.equals(o.getUserId()) && o.isActive()).count();

        // ── Servicios por mes (últimos 12 meses) ──
        List<AdminDtos.MonthlyCount> servicesByMonth = buildMonthlyBookingCounts(completed, 12);

        // ── Ingresos por mes ──
        var paidPayments = paymentRepo.findAll().stream()
                .filter(p -> profId.equals(p.getProfessionalId())
                        && p.getStatus() == PaymentStatus.PAID
                        && p.getPaidAt() != null)
                .collect(Collectors.toList());
        List<AdminDtos.MonthlyAmount> earningsByMonth = buildMonthlyPaymentAmounts(paidPayments, 12);

        // ── Clientes atendidos (únicos) ──
        var clientGroups = completed.stream()
                .collect(Collectors.groupingBy(Booking::getClientId));

        List<AdminDtos.ClientSummary> clients = clientGroups.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().size(), a.getValue().size()))
                .limit(50)
                .map(e -> {
                    Long clientId = e.getKey();
                    var clientBookings = e.getValue();
                    BigDecimal spent = paymentRepo.findAll().stream()
                            .filter(p -> clientId.equals(p.getClientId())
                                    && profId.equals(p.getProfessionalId())
                                    && p.getStatus() == PaymentStatus.PAID)
                            .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    LocalDateTime lastBooking = clientBookings.stream()
                            .map(Booking::getCreatedAt).filter(Objects::nonNull)
                            .max(LocalDateTime::compareTo).orElse(null);

                    return AdminDtos.ClientSummary.builder()
                            .id(clientId)
                            .name(safeGetName(clientId))
                            .photo(safeGetPhoto(clientId))
                            .bookingCount(clientBookings.size())
                            .totalSpent(spent)
                            .lastBooking(lastBooking)
                            .build();
                })
                .collect(Collectors.toList());

        return AdminDtos.ProfessionalDetail.builder()
                .id(profId)
                .name(safeGetName(profId))
                .photo(safeGetPhoto(profId))
                .email(user.getEmail())
                .phone(profile != null ? profile.getPhone() : null)
                .city(profile != null ? profile.getCity() : null)
                .avgRating(rating)
                .reviewCount(reviewCount)
                .totalBookings(allBookings.size())
                .completedBookings(completed.size())
                .cancelledBookings(cancelled)
                .totalEarned(earned)
                .availableBalance(balance)
                .activeOffers(activeOffers)
                .memberSince(user.getCreatedAt())
                .servicesByMonth(servicesByMonth)
                .earningsByMonth(earningsByMonth)
                .clients(clients)
                .build();
    }

    // ══════════════════════════════════════════════════════════
    // ★★★ NUEVOS — RANKING CLIENTES ★★★
    // ══════════════════════════════════════════════════════════

    public List<AdminDtos.ClientRanking> getClientRanking(int limit) {
        var allBookings = bookingRepo.findAll();
        var completedStatuses = Set.of(BookingStatus.COMPLETED, BookingStatus.PAID);

        var byClient = allBookings.stream()
                .collect(Collectors.groupingBy(Booking::getClientId));

        return byClient.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().size(), a.getValue().size()))
                .limit(limit)
                .map(e -> {
                    Long clientId = e.getKey();
                    var clientBookings = e.getValue();
                    long completedCount = clientBookings.stream()
                            .filter(b -> completedStatuses.contains(b.getStatus())).count();
                    long uniquePros = clientBookings.stream()
                            .map(Booking::getProfessionalId).distinct().count();

                    BigDecimal totalSpent = paymentRepo.findAll().stream()
                            .filter(p -> clientId.equals(p.getClientId())
                                    && p.getStatus() == PaymentStatus.PAID)
                            .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    // Rating promedio que el cliente ha dado
                    Double avgGiven = null;
                    try { avgGiven = reviewRepo.getAverageRatingByReviewer(clientId); }
                    catch (Exception ignored) {}

                    var user = authRepo.findById(clientId).orElse(null);
                    LocalDateTime lastBooking = clientBookings.stream()
                            .map(Booking::getCreatedAt).filter(Objects::nonNull)
                            .max(LocalDateTime::compareTo).orElse(null);

                    return AdminDtos.ClientRanking.builder()
                            .id(clientId)
                            .name(safeGetName(clientId))
                            .photo(safeGetPhoto(clientId))
                            .email(safeGetEmail(clientId))
                            .totalBookings(clientBookings.size())
                            .completedBookings(completedCount)
                            .totalSpent(totalSpent)
                            .uniqueProfessionals(uniquePros)
                            .avgRatingGiven(avgGiven)
                            .memberSince(user != null ? user.getCreatedAt() : null)
                            .lastBooking(lastBooking)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════
    // ★★★ NUEVOS — ESTADÍSTICAS MENSUALES ★★★
    // ══════════════════════════════════════════════════════════

    public AdminDtos.MonthlyStats getMonthlyStats(int months) {
        var allBookings = bookingRepo.findAll();
        var allPayments = paymentRepo.findAll();
        var allUsers    = authRepo.findAll();
        var completedStatuses = Set.of(BookingStatus.COMPLETED, BookingStatus.PAID);

        var completedBookings = allBookings.stream()
                .filter(b -> completedStatuses.contains(b.getStatus()))
                .collect(Collectors.toList());
        var paidPayments = allPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID && p.getPaidAt() != null)
                .collect(Collectors.toList());

        YearMonth now = YearMonth.now();
        YearMonth prev = now.minusMonths(1);

        // ── Comparaciones mes actual vs anterior ──
        long bookingsThisMonth = completedBookings.stream()
                .filter(b -> b.getCreatedAt() != null && YearMonth.from(b.getCreatedAt()).equals(now)).count();
        long bookingsPrevMonth = completedBookings.stream()
                .filter(b -> b.getCreatedAt() != null && YearMonth.from(b.getCreatedAt()).equals(prev)).count();

        BigDecimal revenueThisMonth = paidPayments.stream()
                .filter(p -> YearMonth.from(p.getPaidAt()).equals(now))
                .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal revenuePrevMonth = paidPayments.stream()
                .filter(p -> YearMonth.from(p.getPaidAt()).equals(prev))
                .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal feesThisMonth = paidPayments.stream()
                .filter(p -> YearMonth.from(p.getPaidAt()).equals(now))
                .map(p -> p.getPlatformFee() != null ? p.getPlatformFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal feesPrevMonth = paidPayments.stream()
                .filter(p -> YearMonth.from(p.getPaidAt()).equals(prev))
                .map(p -> p.getPlatformFee() != null ? p.getPlatformFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long usersThisMonth = allUsers.stream()
                .filter(u -> u.getCreatedAt() != null && YearMonth.from(u.getCreatedAt()).equals(now)).count();
        long usersPrevMonth = allUsers.stream()
                .filter(u -> u.getCreatedAt() != null && YearMonth.from(u.getCreatedAt()).equals(prev)).count();

        // ── Desglose mensual ──
        List<AdminDtos.MonthlyCount> bookingsByMonth = buildMonthlyBookingCounts(completedBookings, months);
        List<AdminDtos.MonthlyAmount> revenueByMonth = buildMonthlyPaymentAmounts(paidPayments, months);

        List<AdminDtos.MonthlyAmount> feesByMonth = new ArrayList<>();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            String key = ym.toString();
            String label = ym.getMonth().getDisplayName(TextStyle.SHORT, new Locale("es")) + " " + ym.getYear();
            BigDecimal fees = paidPayments.stream()
                    .filter(p -> YearMonth.from(p.getPaidAt()).equals(ym))
                    .map(p -> p.getPlatformFee() != null ? p.getPlatformFee() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            feesByMonth.add(AdminDtos.MonthlyAmount.builder().month(key).label(label).amount(fees).build());
        }

        List<AdminDtos.MonthlyCount> newUsersByMonth = new ArrayList<>();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            String key = ym.toString();
            String label = ym.getMonth().getDisplayName(TextStyle.SHORT, new Locale("es")) + " " + ym.getYear();
            long count = allUsers.stream()
                    .filter(u -> u.getCreatedAt() != null && YearMonth.from(u.getCreatedAt()).equals(ym)).count();
            newUsersByMonth.add(AdminDtos.MonthlyCount.builder().month(key).label(label).count(count).build());
        }

        // ── Por categoría ──
        List<AdminDtos.CategoryCount> byCategory = completedBookings.stream()
                .collect(Collectors.groupingBy(b -> {
                    try {
                        var offer = offerRepo.findById(b.getServiceOfferId());
                        return offer.map(o -> o.getCategory() != null ? o.getCategory().getName() : "Sin categoría")
                                .orElse("Sin categoría");
                    } catch (Exception e) { return "Sin categoría"; }
                }))
                .entrySet().stream()
                .map(e -> AdminDtos.CategoryCount.builder()
                        .category(e.getKey())
                        .count(e.getValue().size())
                        .revenue(BigDecimal.ZERO) // simplificado
                        .build())
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());

        // ── Por método de pago ──
        List<AdminDtos.PaymentMethodCount> paymentMethods = paidPayments.stream()
                .collect(Collectors.groupingBy(p ->
                        p.getPaymentMethod() != null ? p.getPaymentMethod().name() : "UNKNOWN"))
                .entrySet().stream()
                .map(e -> AdminDtos.PaymentMethodCount.builder()
                        .method(e.getKey())
                        .count(e.getValue().size())
                        .amount(e.getValue().stream()
                                .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                        .build())
                .collect(Collectors.toList());

        return AdminDtos.MonthlyStats.builder()
                .bookings(buildComparison("Reservas", bookingsThisMonth, bookingsPrevMonth))
                .revenue(buildComparison("Ingresos", revenueThisMonth.doubleValue(), revenuePrevMonth.doubleValue()))
                .newUsers(buildComparison("Nuevos usuarios", usersThisMonth, usersPrevMonth))
                .platformFees(buildComparison("Comisión", feesThisMonth.doubleValue(), feesPrevMonth.doubleValue()))
                .bookingsByMonth(bookingsByMonth)
                .revenueByMonth(revenueByMonth)
                .feesByMonth(feesByMonth)
                .newUsersByMonth(newUsersByMonth)
                .bookingsByCategory(byCategory)
                .paymentMethods(paymentMethods)
                .build();
    }

    // ══════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════

    private List<AdminDtos.MonthlyCount> buildMonthlyBookingCounts(List<Booking> bookings, int months) {
        YearMonth now = YearMonth.now();
        List<AdminDtos.MonthlyCount> result = new ArrayList<>();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            String key = ym.toString();
            String label = ym.getMonth().getDisplayName(TextStyle.SHORT, new Locale("es")) + " " + ym.getYear();
            long count = bookings.stream()
                    .filter(b -> b.getCreatedAt() != null && YearMonth.from(b.getCreatedAt()).equals(ym)).count();
            result.add(AdminDtos.MonthlyCount.builder().month(key).label(label).count(count).build());
        }
        return result;
    }

    private List<AdminDtos.MonthlyAmount> buildMonthlyPaymentAmounts(
            List<com.goservi.payment.entity.Payment> payments, int months) {
        YearMonth now = YearMonth.now();
        List<AdminDtos.MonthlyAmount> result = new ArrayList<>();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            String key = ym.toString();
            String label = ym.getMonth().getDisplayName(TextStyle.SHORT, new Locale("es")) + " " + ym.getYear();
            BigDecimal amount = payments.stream()
                    .filter(p -> p.getPaidAt() != null && YearMonth.from(p.getPaidAt()).equals(ym))
                    .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.add(AdminDtos.MonthlyAmount.builder().month(key).label(label).amount(amount).build());
        }
        return result;
    }

    private AdminDtos.MonthComparison buildComparison(String label, double current, double previous) {
        double change = previous > 0 ? ((current - previous) / previous) * 100 : (current > 0 ? 100 : 0);
        return AdminDtos.MonthComparison.builder()
                .label(label).current(current).previous(previous)
                .changePercent(Math.round(change * 10.0) / 10.0)
                .build();
    }

    private AdminDtos.TicketAdminView toTicketView(SupportTicket t) {
        var user = authRepo.findById(t.getUserId()).orElse(null);
        return AdminDtos.TicketAdminView.builder()
                .id(t.getId())
                .userName(user != null ? user.getName() : "Usuario #" + t.getUserId())
                .userEmail(user != null ? user.getEmail() : null)
                .userRole(user != null ? user.getActiveRole().name() : null)
                .bookingId(t.getBookingId())
                .type(t.getType())
                .description(t.getDescription())
                .status(t.getStatus())
                .adminResponse(t.getAdminResponse())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private AdminDtos.UserAdminView toUserView(AuthUser u) {
        var profile = profileRepo.findByAuthUserId(u.getId()).orElse(null);
        Double rating = null;
        Long reviewCount = null;
        try {
            rating      = reviewRepo.getAverageRating(u.getId());
            reviewCount = reviewRepo.getReviewCount(u.getId());
        } catch (Exception ignored) {}

        return AdminDtos.UserAdminView.builder()
                .id(u.getId())
                .email(u.getEmail())
                .name(u.getName())
                .active(u.isActive())
                .createdAt(u.getCreatedAt())
                .phone(profile != null ? profile.getPhone() : null)
                .build();
    }

    public Long getUserIdFromDetails(UserDetails userDetails) {
        return authRepo.findByEmail(userDetails.getUsername())
                .map(AuthUser::getId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private String safeGetName(Long userId) {
        if (userId == null) return null;
        try {
            return profileRepo.findByAuthUserId(userId)
                    .map(p -> p.getFullName())
                    .orElseGet(() -> authRepo.findById(userId)
                            .map(AuthUser::getName)
                            .orElse("Usuario #" + userId));
        } catch (Exception e) {
            return "Usuario #" + userId;
        }
    }

    private String safeGetEmail(Long userId) {
        if (userId == null) return null;
        try {
            return authRepo.findById(userId)
                    .map(AuthUser::getEmail)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String safeGetPhoto(Long userId) {
        if (userId == null) return null;
        try {
            return profileRepo.findByAuthUserId(userId)
                    .map(p -> p.getPhotoUrl())
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private long countByStatus(List<Booking> bookings, BookingStatus status) {
        return bookings.stream().filter(b -> b.getStatus() == status).count();
    }
}