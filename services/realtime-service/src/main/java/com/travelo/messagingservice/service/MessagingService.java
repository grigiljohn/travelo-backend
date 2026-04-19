package com.travelo.messagingservice.service;

import com.travelo.messagingservice.dto.*;

import java.util.List;
import java.util.UUID;

public interface MessagingService {
    
    ConversationDto createConversation(CreateConversationRequest request);
    
    List<ConversationDto> getConversations(UUID userId, int page, int limit);
    
    ConversationDto getConversation(UUID conversationId, UUID userId);
    
    ConversationDto getConversationInfo(UUID conversationId, UUID userId);
    
    void deleteConversation(UUID conversationId, UUID userId);
    
    MessageDto sendMessage(CreateMessageRequest request);

    /**
     * @param participantUserIdOrNullIfSystem if null, only use from trusted loopback (no participant check)
     */
    MessageDto getMessage(UUID conversationId, UUID messageId, UUID participantUserIdOrNullIfSystem);

    void markMessageDelivered(UUID messageId, UUID userId);

    void markMessageRead(UUID messageId, UUID userId);
    
    List<MessageDto> getMessages(UUID conversationId, UUID userId, int page, int limit);
    
    void markAsRead(UUID conversationId, UUID userId);
    
    void deleteMessage(UUID conversationId, UUID messageId, UUID userId);
    
    void addReaction(UUID conversationId, UUID messageId, UUID userId, String emoji);
    
    void removeReaction(UUID conversationId, UUID messageId, UUID userId, String emoji);
    
    List<MessageDto> searchMessages(UUID conversationId, UUID userId, String query, int page, int limit);
    
    /**
     * @param authorization optional {@code Authorization} header value (e.g. {@code Bearer ...}) to forward to user-service
     */
    List<ChatUserDto> searchUsers(String query, int page, int limit, UUID viewerId, String authorization);
    
    Long getTotalUnreadCount(UUID userId);
}

