package com.goservi.auth.service.impl;

import com.goservi.auth.dto.AuthDtos;
import com.goservi.auth.entity.AuthUser;
import com.goservi.auth.entity.Role;
import com.goservi.auth.repository.AuthUserRepository;
import com.goservi.auth.service.AuthService;
import com.goservi.common.dto.NotificationRequest;
import com.goservi.common.dto.NotificationType;
import com.goservi.common.exception.BadRequestException;
import com.goservi.common.exception.NotFoundException;
import com.goservi.common.exception.UnauthorizedException;
import com.goservi.common.security.JwtUtil;
import com.goservi.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final AuthUserRepository repo;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder encoder;
    private final NotificationService notificationService;

    @Override
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest req) {
        if (repo.existsByEmail(req.getEmail())) {
            throw new BadRequestException("El email ya está registrado");
        }

        Role role = req.getRole() != null ? req.getRole() : Role.CLIENT;

        AuthUser user = AuthUser.builder()
                .email(req.getEmail())
                .password(encoder.encode(req.getPassword()))
                .name(req.getName())
                .phone(req.getPhone())
                .activeRole(role)
                .roles(new ArrayList<>())
                .build();
        user.getRoles().add(role);
        repo.save(user);

        try {
            notificationService.send(NotificationRequest.builder()
                    .to(user.getEmail())
                    .type(NotificationType.EMAIL)
                    .subject("¡Bienvenido a GoServi!")
                    .message("Hola " + user.getName() + ", tu cuenta ha sido creada exitosamente.")
                    .metadata(Map.of("event", "REGISTER", "userId", String.valueOf(user.getId())))
                    .build());
        } catch (Exception e) {
            log.warn("No se pudo enviar email de bienvenida: {}", e.getMessage());
        }

        return buildResponse(user, jwtUtil.generateToken(user.getEmail(), Set.of(role.name())));
    }

    @Override
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req) {
        AuthUser user = repo.findByEmail(req.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Credenciales inválidas"));

        if (!encoder.matches(req.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Credenciales inválidas");
        }
        if (!user.isActive()) {
            throw new UnauthorizedException("Cuenta desactivada");
        }

        var roleNames = user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
        return buildResponse(user, jwtUtil.generateToken(user.getEmail(), roleNames));
    }

    @Override
    public AuthDtos.AuthResponse switchRole(AuthDtos.SwitchRoleRequest req, String email) {
        AuthUser user = repo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Role targetRole = req.getNewRole();
        if (targetRole == null) {
            throw new BadRequestException("El rol solicitado no es válido");
        }

        // ✅ Si el usuario no tiene el rol solicitado, se lo asignamos automáticamente
        // Cualquier usuario puede ser cliente o profesional en GoServi
        if (!user.getRoles().contains(targetRole)) {
            user.getRoles().add(targetRole);
            log.info("Rol {} asignado al usuario {}", targetRole, email);
        }

        user.setActiveRole(targetRole);
        repo.save(user);

        var roleNames = user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
        return buildResponse(user, jwtUtil.generateToken(user.getEmail(), roleNames));
    }

    @Override
    public void changePassword(String email, String currentPassword, String newPassword) {
        AuthUser user = repo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        if (!encoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("La contraseña actual es incorrecta");
        }

        user.setPassword(encoder.encode(newPassword));
        repo.save(user);
        log.info("Contraseña actualizada para usuario {}", email);
    }

    @Override
    public boolean userExists(Long id) {
        return repo.existsById(id);
    }

    private AuthDtos.AuthResponse buildResponse(AuthUser user, String token) {
        AuthDtos.AuthResponse res = new AuthDtos.AuthResponse();
        res.setUserId(user.getId());
        res.setEmail(user.getEmail());
        res.setName(user.getName());
        res.setActiveRole(user.getActiveRole().name());
        res.setToken(token);
        return res;
    }
}