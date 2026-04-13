package com.travelo.messagingservice.controller;

import com.travelo.commons.security.SecurityUtils;
import com.travelo.messagingservice.dto.CreateMessageRequest;
import com.travelo.messagingservice.dto.MessageDto;
import com.travelo.messagingservice.dto.SendMessageCompatRequest;
import com.travelo.messagingservice.service.MessagingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Optional REST aliases for clients expecting /messages/* paths. Canonical APIs remain under
 * {@link MessagingController} (/conversations/...).
 */
@RestController
@RequestMapping("/api/v1/messages")
@Tag(name = "Messaging (compat)", description = "Backward-compatible message endpoints")
public class MessagingCompatController {

    private static final Logger logger = LoggerFactory.getLogger(MessagingCompatController.class);

    private final MessagingService messagingService;

    public MessagingCompatController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @PostMapping("/send")
    @Operation(summary = "Send message (compat)", description = "Same as POST /api/v1/conversations/{id}/messages")
    public ResponseEntity<MessageDto> sendCompat(@Valid @RequestBody SendMessageCompatRequest body) {
        UUID senderId = SecurityUtils.getCurrentUserId();
        if (senderId == null) {
            throw new IllegalArgumentException("User not authenticated");
        }
        CreateMessageRequest req = new CreateMessageRequest();
        req.setConversationId(body.getConversationId());
        req.setSenderId(senderId);
        req.setContent(body.getContent() != null ? body.getContent() : "");
        req.setMessageType(body.getMessageType());
        req.setAttachmentUrl(body.getAttachmentUrl());
        req.setAttachmentMetadata(body.getAttachmentMetadata());
        req.setReplyToId(body.getReplyToId());
        logger.info("POST /api/v1/messages/send conversationId={}", body.getConversationId());
        return ResponseEntity.ok(messagingService.sendMessage(req));
    }

    @GetMapping("/conversation/{conversationId}")
    @Operation(summary = "List messages (compat)", description = "Same as GET /api/v1/conversations/{id}/messages")
    public ResponseEntity<List<MessageDto>> listCompat(
            @PathVariable UUID conversationId,
            @RequestParam(value = "user_id", required = false) UUID userId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "50") @Max(100) int limit) {
        UUID uid = userId != null ? userId : SecurityUtils.getCurrentUserId();
        if (uid == null) {
            throw new IllegalArgumentException("user_id or JWT required");
        }
        return ResponseEntity.ok(messagingService.getMessages(conversationId, uid, page, limit));
    }
}
