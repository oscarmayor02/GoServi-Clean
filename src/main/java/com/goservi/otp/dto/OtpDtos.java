package com.goservi.otp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

public class OtpDtos {

    @Data
    public static class SendOtpRequest {
        @NotBlank
        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Teléfono inválido")
        private String phone;
    }

    @Data
    public static class VerifyOtpRequest {
        @NotBlank private String phone;
        @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "El código debe ser de 6 dígitos")
        private String code;
    }

    @Data
    public static class OtpResponse {
        private boolean success;
        private String message;
        private int remainingAttempts;
    }
}
