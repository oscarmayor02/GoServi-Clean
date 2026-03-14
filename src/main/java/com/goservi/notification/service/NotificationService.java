package com.goservi.notification.service;

import com.goservi.common.dto.NotificationRequest;

public interface NotificationService {
    void send(NotificationRequest request);
}
