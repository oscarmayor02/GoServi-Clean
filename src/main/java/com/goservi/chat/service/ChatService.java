package com.goservi.chat.service;

import com.goservi.chat.dto.ChatDtos;
import com.goservi.chat.entity.*;
import com.goservi.chat.repository.ChatMessageRepository;
import com.goservi.chat.repository.ChatThreadRepository;
import com.goservi.common.exception.BadRequestException;
import com.goservi.common.exception.NotFoundException;
import com.goservi.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatThreadRepository threadRepo;
    private final ChatMessageRepository msgRepo;
    private final UserProfileService userProfileService;
    private final SimpMessagingTemplate ws;

    public ChatDtos.ThreadResponse getOrCreateThread(Long requesterId, ChatDtos.CreateThreadRequest req) {
        if (req.getBookingId() != null) {
            var existing = threadRepo.findByBookingId(req.getBookingId());
            if (existing.isPresent()) return toThreadResponse(existing.get(), requesterId);
        }

        var existing = threadRepo.findByClientIdAndProfessionalIdAndServiceOfferIdAndKind(
                requesterId, req.getProfessionalId(), req.getServiceOfferId(), ThreadKind.INQUIRY);
        if (existing.isPresent()) return toThreadResponse(existing.get(), requesterId);

        boolean isWork = req.getBookingId() != null;
        ChatThread thread = ChatThread.builder()
                .clientId(requesterId)
                .professionalId(req.getProfessionalId())
                .serviceOfferId(req.getServiceOfferId())
                .bookingId(req.getBookingId())
                .kind(isWork ? ThreadKind.WORK : ThreadKind.INQUIRY)
                .status(ThreadStatus.OPEN)
                .build();

        return toThreadResponse(threadRepo.save(thread), requesterId);
    }

    public List<ChatDtos.ThreadResponse> getMyThreads(Long userId) {
        return threadRepo.findByClientIdOrProfessionalId(userId, userId).stream()
                .map(t -> toThreadResponse(t, userId))
                .collect(Collectors.toList());
    }

    public List<ChatDtos.MessageResponse> getMessages(Long threadId, Long userId) {
        var thread = threadRepo.findById(threadId)
                .orElseThrow(() -> new NotFoundException("Hilo no encontrado"));
        if (!thread.getClientId().equals(userId) && !thread.getProfessionalId().equals(userId))
            throw new BadRequestException("Sin acceso a este hilo");

        return msgRepo.findByThreadIdOrderBySentAtAsc(threadId).stream()
                .map(m -> toMsgResponse(m, userId))
                .collect(Collectors.toList());
    }

    public ChatDtos.MessageResponse sendMessage(Long threadId, Long senderId, ChatDtos.SendMessageRequest req) {
        var thread = threadRepo.findById(threadId)
                .orElseThrow(() -> new NotFoundException("Hilo no encontrado"));

        if (thread.getStatus() == ThreadStatus.CLOSED)
            throw new BadRequestException("Este hilo está cerrado");

        if (!thread.getClientId().equals(senderId) && !thread.getProfessionalId().equals(senderId))
            throw new BadRequestException("Sin acceso a este hilo");

        ChatMessage msg = ChatMessage.builder()
                .threadId(threadId)
                .senderId(senderId)
                .content(req.getContent())
                .build();

        // ✅ saveAndFlush: garantiza que id y sentAt estén populados antes del broadcast
        var saved = msgRepo.saveAndFlush(msg);

        // ✅ Fallback si @CreationTimestamp aún no populó
        LocalDateTime sentAt = saved.getSentAt() != null ? saved.getSentAt() : LocalDateTime.now();

        // Broadcast via WebSocket
        try {
            var summary = userProfileService.getSummary(senderId);
            var wsMsg = ChatDtos.WsMessage.builder()
                    .id(saved.getId())          // ✅ CRÍTICO: el frontend necesita el id para dedup
                    .threadId(threadId)
                    .senderId(senderId)
                    .senderName(summary.getFullName())
                    .senderPhoto(summary.getPhotoUrl())
                    .content(req.getContent())
                    .sentAt(sentAt)
                    .build();
            ws.convertAndSend("/topic/chat/" + threadId, wsMsg);
        } catch (Exception e) {
            log.warn("WS broadcast failed: {}", e.getMessage());
        }

        return toMsgResponse(saved, senderId);
    }

    public void markRead(Long threadId, Long userId) {
        msgRepo.findByThreadIdOrderBySentAtAsc(threadId).forEach(m -> {
            if (!m.getSenderId().equals(userId) && m.getReadAt() == null) {
                m.setReadAt(LocalDateTime.now());
                msgRepo.save(m);
            }
        });
    }

    @Transactional
    public void closeThreadByBookingId(String bookingId) {
        if (bookingId == null || bookingId.isBlank()) return;

        threadRepo.findByBookingId(bookingId).ifPresent(thread -> {
            if (thread.getStatus() == ThreadStatus.CLOSED) return; // idempotente
            thread.setStatus(ThreadStatus.CLOSED);
            thread.setClosedAt(java.time.LocalDateTime.now());
            threadRepo.save(thread);
            log.info("Thread {} cerrado — booking {} pagado", thread.getId(), bookingId);
        });
    }

    private ChatDtos.MessageResponse toMsgResponse(ChatMessage m, Long currentUserId) {
        String name = null;
        String photo = null;
        try {
            var s = userProfileService.getSummary(m.getSenderId());
            name = s.getFullName();
            photo = s.getPhotoUrl();
        } catch (Exception ignored) {}

        return ChatDtos.MessageResponse.builder()
                .id(m.getId())
                .threadId(m.getThreadId())
                .senderId(m.getSenderId())
                .senderName(name)
                .senderPhoto(photo)
                .content(m.getContent())
                .sentAt(m.getSentAt())
                .readAt(m.getReadAt())
                .build();
    }

    private ChatDtos.ThreadResponse toThreadResponse(ChatThread t, Long currentUserId) {
        Long otherPartyId = t.getClientId().equals(currentUserId) ? t.getProfessionalId() : t.getClientId();
        String otherName = null;
        String otherPhoto = null;
        try {
            var s = userProfileService.getSummary(otherPartyId);
            otherName = s.getFullName();
            otherPhoto = s.getPhotoUrl();
        } catch (Exception ignored) {}

        long unread = msgRepo.countByThreadIdAndReadAtIsNull(t.getId());
        ChatDtos.MessageResponse lastMsg = null;
        var messages = msgRepo.findByThreadIdOrderBySentAtAsc(t.getId());
        if (!messages.isEmpty()) lastMsg = toMsgResponse(messages.get(messages.size() - 1), currentUserId);

        return ChatDtos.ThreadResponse.builder()
                .id(t.getId())
                .clientId(t.getClientId())
                .professionalId(t.getProfessionalId())
                .serviceOfferId(t.getServiceOfferId())
                .bookingId(t.getBookingId())
                .kind(t.getKind().name())
                .status(t.getStatus().name())
                .openedAt(t.getOpenedAt())
                .otherPartyName(otherName)
                .otherPartyPhoto(otherPhoto)
                .unreadCount(unread)
                .lastMessage(lastMsg)
                .build();
    }
}