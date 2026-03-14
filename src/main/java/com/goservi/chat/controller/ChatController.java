package com.goservi.chat.controller;

import com.goservi.auth.repository.AuthUserRepository;
import com.goservi.chat.dto.ChatDtos;
import com.goservi.chat.service.ChatService;
import com.goservi.common.exception.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Mensajería entre cliente y profesional")
public class ChatController {

    private final ChatService chatService;
    private final AuthUserRepository authRepo;

    @Operation(summary = "Obtener o crear hilo de chat")
    @PostMapping("/threads")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatDtos.ThreadResponse> getOrCreateThread(
            @RequestBody ChatDtos.CreateThreadRequest req,
            Principal principal) {
        return ResponseEntity.ok(chatService.getOrCreateThread(getAuthUserId(principal), req));
    }

    @Operation(summary = "Mis hilos de chat")
    @GetMapping("/threads")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatDtos.ThreadResponse>> myThreads(Principal principal) {
        return ResponseEntity.ok(chatService.getMyThreads(getAuthUserId(principal)));
    }

    @Operation(summary = "Mensajes de un hilo")
    @GetMapping("/threads/{threadId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatDtos.MessageResponse>> getMessages(
            @PathVariable Long threadId, Principal principal) {
        return ResponseEntity.ok(chatService.getMessages(threadId, getAuthUserId(principal)));
    }

    @Operation(summary = "Enviar mensaje (REST)")
    @PostMapping("/threads/{threadId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatDtos.MessageResponse> sendMessage(
            @PathVariable Long threadId,
            @RequestBody ChatDtos.SendMessageRequest req,
            Principal principal) {
        return ResponseEntity.ok(chatService.sendMessage(threadId, getAuthUserId(principal), req));
    }

    @Operation(summary = "Marcar mensajes como leídos")
    @PostMapping("/threads/{threadId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markRead(@PathVariable Long threadId, Principal principal) {
        chatService.markRead(threadId, getAuthUserId(principal));
        return ResponseEntity.ok().build();
    }

    // WebSocket message handler
    @MessageMapping("/chat.send")
    public void handleWsMessage(@Payload ChatDtos.SendMessageRequest req, Principal principal) {
        // Handled via REST for simplicity; WS used for real-time delivery only
    }

    private Long getAuthUserId(Principal principal) {
        return authRepo.findByEmail(principal.getName())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"))
                .getId();
    }
}
