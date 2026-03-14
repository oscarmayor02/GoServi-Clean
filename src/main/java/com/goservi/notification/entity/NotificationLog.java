package com.goservi.notification.entity;

import com.goservi.common.dto.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_logs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    private String subject;

    @Column(length = 2000)
    private String message;

    @Column(nullable = false)
    private boolean success;

    private String errorMessage;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime sentAt;
}
