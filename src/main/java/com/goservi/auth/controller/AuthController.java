package com.goservi.auth.controller;

import com.goservi.auth.dto.AuthDtos;
import com.goservi.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registro, login y gestión de roles")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Registrar nuevo usuario")
    @PostMapping("/register")
    public ResponseEntity<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @Operation(summary = "Login")
    @PostMapping("/login")
    public ResponseEntity<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @Operation(summary = "Cambiar rol activo (CLIENT ↔ PROFESSIONAL)")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/switch-role")
    public ResponseEntity<AuthDtos.AuthResponse> switchRole(
            @RequestBody AuthDtos.SwitchRoleRequest req,
            Principal principal) {
        return ResponseEntity.ok(authService.switchRole(req, principal.getName()));
    }

    @Operation(summary = "Cambiar contraseña")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody AuthDtos.ChangePasswordRequest req,
            Principal principal) {
        authService.changePassword(principal.getName(), req.getCurrentPassword(), req.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente"));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> me(Principal principal) {
        return ResponseEntity.ok(principal.getName());
    }
}