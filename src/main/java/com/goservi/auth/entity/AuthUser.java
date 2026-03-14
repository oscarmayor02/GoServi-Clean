package com.goservi.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "auth_users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AuthUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(name = "phone")
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role activeRole = Role.CLIENT;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "social_provider")
    private String socialProvider;

    @Column(name = "social_id")
    private String socialId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // User can have CLIENT and PROFESSIONAL roles
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "auth_user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @Builder.Default
    private List<Role> roles = new ArrayList<>();
}
