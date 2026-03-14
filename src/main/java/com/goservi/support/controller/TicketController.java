package com.goservi.support.controller;

import com.goservi.admin.dto.AdminDtos;
import com.goservi.admin.service.AdminService;
import com.goservi.support.entity.SupportTicket;
import com.goservi.support.repository.SupportTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Endpoints para CLIENTES y PROFESIONALES — crear y ver sus tickets */
@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final SupportTicketRepository ticketRepo;
    private final AdminService adminService;

    /** Cliente/profesional crea un ticket */
    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody AdminDtos.TicketCreateRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = adminService.getUserIdFromDetails(userDetails);

        SupportTicket ticket = SupportTicket.builder()
                .userId(userId)
                .bookingId(req.getBookingId())
                .type(req.getType())
                .description(req.getDescription())
                .status("OPEN")
                .build();

        ticketRepo.save(ticket);
        return ResponseEntity.ok(Map.of(
                "message", "Ticket creado. Te responderemos pronto.",
                "ticketId", ticket.getId()
        ));
    }

    /** Cliente/profesional ve sus propios tickets */
    @GetMapping("/my")
    public ResponseEntity<List<SupportTicket>> myTickets(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = adminService.getUserIdFromDetails(userDetails);
        return ResponseEntity.ok(ticketRepo.findByUserIdOrderByCreatedAtDesc(userId));
    }

    /** Ver detalle de un ticket propio */
    @GetMapping("/{id}")
    public ResponseEntity<SupportTicket> getTicket(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = adminService.getUserIdFromDetails(userDetails);
        SupportTicket t = ticketRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));
        if (!t.getUserId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(t);
    }
}