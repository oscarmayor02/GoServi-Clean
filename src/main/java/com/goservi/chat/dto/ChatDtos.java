package com.goservi.chat.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class ChatDtos {

    @Data
    public static class CreateThreadRequest {
        private Long professionalId;
        private Long serviceOfferId;
        private String bookingId;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ThreadResponse {
        private Long id;
        private Long clientId;
        private Long professionalId;
        private Long serviceOfferId;
        private String bookingId;
        private String kind;
        private String status;
        private LocalDateTime openedAt;
        private String otherPartyName;
        private String otherPartyPhoto;
        private long unreadCount;
        private MessageResponse lastMessage;
    }

    @Data
    public static class SendMessageRequest {
        private String content;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MessageResponse {
        private Long id;
        private Long threadId;
        private Long senderId;
        private String senderName;
        private String senderPhoto;
        private String content;
        private LocalDateTime sentAt;
        private LocalDateTime readAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WsMessage {
        private Long id;          // ← AGREGAR ESTE CAMPO (era el que faltaba)
        private Long threadId;
        private Long senderId;
        private String senderName;
        private String senderPhoto;
        private String content;
        private java.time.LocalDateTime sentAt;
    }

}
