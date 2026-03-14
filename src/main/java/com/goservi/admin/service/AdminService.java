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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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

    // ── DASHBOARD ──────────────────────────────────────────────

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
            String date = LocalDate.now().minusDays(i).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
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

    // ── USUARIOS ───────────────────────────────────────────────

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

    // ── RESERVAS ───────────────────────────────────────────────

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
                            // usa los campos que SÍ existen en BookingAdminView
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

    // ── PAGOS ──────────────────────────────────────────────────

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

    // ── RETIROS ────────────────────────────────────────────────

    public List<AdminDtos.WithdrawalAdminView> getAllWithdrawals(String status) {
        var list = status != null
                ? withdrawalRepo.findByStatus(WithdrawalStatus.valueOf(status.toUpperCase()))
                : withdrawalRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        return list.stream().map(w -> AdminDtos.WithdrawalAdminView.builder()
                        .id(Long.valueOf(w.getId()))          // UUID → String, convierte si tu PK es String
                        .professionalId(w.getProfessionalId())
                        .professionalName(safeGetName(w.getProfessionalId()))
                        .professionalEmail(safeGetEmail(w.getProfessionalId()))
                        .amount(w.getAmount())
                        .bankAccount(w.getAccountNumber())    // accountNumber en la entity
                        .bankName(w.getBankName())
                        .status(w.getStatus().name())
                        .rejectionReason(w.getAdminNote())    // adminNote en la entity
                        .createdAt(w.getCreatedAt())
                        .processedAt(w.getProcessedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void processWithdrawal(Long id) {
        var w = withdrawalRepo.findById(String.valueOf(id))
                .orElseThrow(() -> new NotFoundException("Retiro no encontrado"));

        if (w.getStatus() == WithdrawalStatus.PROCESSED) {
            throw new IllegalStateException("El retiro ya fue procesado");
        }

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
    public void rejectWithdrawal(Long id, String reason) {
        var w = withdrawalRepo.findById(String.valueOf(id))
                .orElseThrow(() -> new NotFoundException("Retiro no encontrado"));

        if (w.getStatus() == WithdrawalStatus.PROCESSED) {
            throw new IllegalStateException("No se puede rechazar un retiro ya procesado");
        }

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

    // ── TICKETS ────────────────────────────────────────────────

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

    // ── REPORTES ───────────────────────────────────────────────

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
            String dateStr = cursor.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
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

    // ── SERVICIOS ──────────────────────────────────────────────

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

    // ── HELPERS ────────────────────────────────────────────────

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
            return userProfileService.getSummary(userId).getFullName();
        } catch (Exception e) {
            return "Usuario #" + userId;
        }
    }

    private String safeGetEmail(Long userId) {
        if (userId == null) return null;
        try {
            return authRepo.findById(userId).map(AuthUser::getEmail).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private long countByStatus(List<Booking> bookings, BookingStatus status) {
        return bookings.stream().filter(b -> b.getStatus() == status).count();
    }
}