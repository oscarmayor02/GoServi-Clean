package com.goservi.auth.dto;

import com.goservi.auth.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthDtos {

    @Data
    public static class RegisterRequest {
        @Email @NotBlank
        private String email;
        @NotBlank @Size(min = 6)
        private String password;
        @NotBlank
        private String name;
        private String phone;
        private Role role = Role.CLIENT;
    }

    @Data
    public static class LoginRequest {
        @Email @NotBlank
        private String email;
        @NotBlank
        private String password;
    }

    @Data
    public static class SwitchRoleRequest {
        private Role newRole;
    }

    @Data
    public static class AuthResponse {
        private Long userId;
        private String email;
        private String name;
        private String activeRole;
        private String token;
    }

    // ── NUEVO ──────────────────────────────────────────────────
    @Data
    public static class ChangePasswordRequest {
        @NotBlank
        private String currentPassword;
        @NotBlank @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        private String newPassword;
    }
}