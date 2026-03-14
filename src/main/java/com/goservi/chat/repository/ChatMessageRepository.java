package com.goservi.chat.repository;

import com.goservi.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByThreadIdOrderBySentAtAsc(Long threadId);
    long countByThreadIdAndReadAtIsNull(Long threadId);
}
