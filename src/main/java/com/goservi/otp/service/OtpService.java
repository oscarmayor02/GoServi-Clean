package com.goservi.otp.service;

import com.goservi.auth.repository.AuthUserRepository;
import com.goservi.common.dto.NotificationRequest;
import com.goservi.common.dto.NotificationType;
import com.goservi.common.exception.BadRequestException;
import com.goservi.common.exception.NotFoundException;
import com.goservi.notification.service.NotificationService;
import com.goservi.otp.dto.OtpDtos;
import com.goservi.otp.entity.OtpCode;
import com.goservi.otp.repository.OtpCodeRepository;
import com.goservi.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OtpService {

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;
    private static final int MAX_SENDS_PER_HOUR = 5;

    private final OtpCodeRepository otpRepo;
    private final AuthUserRepository authRepo;
    private final UserProfileRepository profileRepo;
    private final NotificationService notificationService;

    public OtpDtos.OtpResponse send(Long userId, OtpDtos.SendOtpRequest req) {
        // Rate limit: máximo 5 envíos por hora
        long recentCount = otpRepo.countRecentByUser(userId, LocalDateTime.now().minusHours(1));
        if (recentCount >= MAX_SENDS_PER_HOUR) {
            throw new BadRequestException("Demasiados intentos. Espera un momento antes de volver a intentarlo.");
        }

        // Invalidar OTPs anteriores
        otpRepo.invalidateAllForUser(userId);

        String code = RandomStringUtils.randomNumeric(6);

        OtpCode otp = OtpCode.builder()
                .userId(userId)
                .phone(req.getPhone())
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .build();
        otpRepo.save(otp);

        // Enviar SMS
        String phone = req.getPhone().startsWith("+") ? req.getPhone() : "+57" + req.getPhone();
        try {
            notificationService.send(NotificationRequest.builder()
                    .to(phone)
                    .type(NotificationType.SMS)
                    .message("Tu código de verificación GoServi es: " + code + ". Válido por 5 minutos.")
                    .build());
            log.info("OTP sent to {}", phone);
        } catch (Exception e) {
            log.error("Failed to send OTP SMS: {}", e.getMessage());
            // No falla el request, el código ya está guardado
        }

        OtpDtos.OtpResponse res = new OtpDtos.OtpResponse();
        res.setSuccess(true);
        res.setMessage("Código enviado a " + maskPhone(phone));
        res.setRemainingAttempts(MAX_ATTEMPTS);
        return res;
    }

    public OtpDtos.OtpResponse verify(Long userId, OtpDtos.VerifyOtpRequest req) {
        OtpCode otp = otpRepo.findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new BadRequestException("No hay código OTP activo. Solicita uno nuevo."));

        if (otp.isUsed()) {
            throw new BadRequestException("El código ya fue utilizado. Solicita uno nuevo.");
        }

        if (LocalDateTime.now().isAfter(otp.getExpiresAt())) {
            otp.setUsed(true);
            otpRepo.save(otp);
            throw new BadRequestException("El código expiró. Solicita uno nuevo.");
        }

        int remaining = MAX_ATTEMPTS - otp.getAttempts() - 1;

        if (!otp.getCode().equals(req.getCode())) {
            otp.setAttempts(otp.getAttempts() + 1);
            if (otp.getAttempts() >= MAX_ATTEMPTS) {
                otp.setUsed(true);
                otpRepo.save(otp);
                throw new BadRequestException("Demasiados intentos fallidos. Solicita un código nuevo.");
            }
            otpRepo.save(otp);
            OtpDtos.OtpResponse res = new OtpDtos.OtpResponse();
            res.setSuccess(false);
            res.setMessage("Código incorrecto.");
            res.setRemainingAttempts(remaining);
            return res;
        }

        // Código correcto
        otp.setUsed(true);
        otp.setVerified(true);
        otpRepo.save(otp);

        // Marcar teléfono como verificado en el perfil
        profileRepo.findByAuthUserId(userId).ifPresent(p -> {
            p.setPhoneVerified(true);
            p.setPhone(req.getPhone());
            profileRepo.save(p);
        });

        OtpDtos.OtpResponse res = new OtpDtos.OtpResponse();
        res.setSuccess(true);
        res.setMessage("Teléfono verificado correctamente.");
        res.setRemainingAttempts(remaining);
        return res;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return phone.substring(0, phone.length() - 4).replaceAll("\\d", "*")
                + phone.substring(phone.length() - 4);
    }
}
