package com.goservi.auth.service;

import com.goservi.auth.dto.AuthDtos;

public interface AuthService {
    AuthDtos.AuthResponse register(AuthDtos.RegisterRequest req);
    AuthDtos.AuthResponse login(AuthDtos.LoginRequest req);
    AuthDtos.AuthResponse switchRole(AuthDtos.SwitchRoleRequest req, String email);
    boolean userExists(Long id);
    void changePassword(String email, String currentPassword, String newPassword);

}
