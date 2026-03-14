package com.goservi.otp.controller;

import com.goservi.auth.repository.AuthUserRepository;
import com.goservi.common.exception.BadRequestException;
import com.goservi.common.exception.NotFoundException;
import com.goservi.otp.dto.OtpDtos;
import com.goservi.otp.service.OtpService;
import com.goservi.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@Tag(name = "OTP", description = "Verificación de número de teléfono")
public class OtpController {

    private final OtpService otpService;
    private final AuthUserRepository authRepo;
    private final UserProfileService userProfileService;

    // ── Endpoints originales /auth/otp ────────────────────────────────────────

    @Operation(summary = "Enviar código OTP al teléfono")
    @PostMapping("/auth/otp/send")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OtpDtos.OtpResponse> send(
            @Valid @RequestBody OtpDtos.SendOtpRequest req,
            Principal principal) {
        Long userId = getAuthUserId(principal);
        return ResponseEntity.ok(otpService.send(userId, req));
    }

    @Operation(summary = "Verificar código OTP")
    @PostMapping("/auth/otp/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OtpDtos.OtpResponse> verify(
            @Valid @RequestBody OtpDtos.VerifyOtpRequest req,
            Principal principal) {
        Long userId = getAuthUserId(principal);
        return ResponseEntity.ok(otpService.verify(userId, req));
    }

    // ── Endpoints nuevos /users/{id} — compatibles con el frontend ────────────
    @PostMapping("/users/{authUserId}/send-otp")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OtpDtos.OtpResponse> sendOtpById(
            @PathVariable Long authUserId) {
        String phone = authRepo.findById(authUserId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"))
                .getPhone();

        if (phone == null || phone.isBlank()) {
            throw new BadRequestException("Este usuario no tiene teléfono registrado. Actualiza tu perfil.");
        }

        OtpDtos.SendOtpRequest req = new OtpDtos.SendOtpRequest();
        req.setPhone(phone);
        return ResponseEntity.ok(otpService.send(authUserId, req));
    }

    @PostMapping("/users/{authUserId}/verify-otp")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OtpDtos.OtpResponse> verifyOtpById(
            @PathVariable Long authUserId,
            @RequestParam String otp) {
        String phone = authRepo.findById(authUserId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"))
                .getPhone();

        if (phone == null || phone.isBlank()) {
            throw new BadRequestException("Este usuario no tiene teléfono registrado. Actualiza tu perfil.");
        }

        OtpDtos.VerifyOtpRequest req = new OtpDtos.VerifyOtpRequest();
        req.setPhone(phone);
        req.setCode(otp);
        return ResponseEntity.ok(otpService.verify(authUserId, req));
    }

    private Long getAuthUserId(Principal principal) {
        return authRepo.findByEmail(principal.getName())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"))
                .getId();
    }
}