package com.travelo.messagingservice.service.impl;

import com.travelo.commons.security.SecurityUtils;
import com.travelo.messagingservice.dto.*;
import com.travelo.messagingservice.entity.*;
import com.travelo.messagingservice.repository.*;
import com.travelo.messagingservice.service.MessagingService;
import com.travelo.websocketservice.service.MessageBroadcastService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Transactional(transactionManager = "messagingTransactionManager")
public class MessagingServiceImpl implements MessagingService {

    private static final Logger logger = LoggerFactory.getLogger(MessagingServiceImpl.class);

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final MessageReadReceiptRepository readReceiptRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RestTemplate restTemplate;

    private MessageBroadcastService messageBroadcastService;

    @Value("${app.user-service.url:http://localhost:8081}")
    private String userServiceUrl;

    @Autowired
    public void setMessageBroadcastService(@Lazy MessageBroadcastService messageBroadcastService) {
        this.messageBroadcastService = messageBroadcastService;
    }

    public MessagingServiceImpl(
            ConversationRepository conversationRepository,
            ConversationParticipantRepository participantRepository,
            MessageRepository messageRepository,
            MessageReadReceiptRepository readReceiptRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            RestTemplate restTemplate) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        this.readReceiptRepository = readReceiptRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.restTemplate = restTemplate;
    }

    @Override
    @Transactional(transactionManager = "messagingTransactionManager")
    public ConversationDto createConversation(CreateConversationRequest request) {
        ConversationType type = ConversationType.valueOf(request.getType());
        
        // For DIRECT conversations, check if one already exists between the participants
        if (type == ConversationType.DIRECT && request.getParticipantIds().size() == 2) {
            UUID currentUserId = SecurityUtils.getCurrentUserId();
            
            if (currentUserId != null) {
                // Find the other participant (the one that's not the current user)
                UUID otherParticipantId = request.getParticipantIds().stream()
                        .filter(id -> !id.equals(currentUserId))
                        .findFirst()
                        .orElse(null);
                
                if (otherParticipantId != null) {
                    // Check for existing direct conversation (order doesn't matter)
                    Conversation existingConversation = conversationRepository.findDirectConversation(
                            currentUserId, otherParticipantId);
                    
                    // Also check reverse order (in case the query doesn't handle both directions)
                    if (existingConversation == null) {
                        existingConversation = conversationRepository.findDirectConversation(
                                otherParticipantId, currentUserId);
                    }
                    
                    if (existingConversation != null) {
                        logger.info("Found existing direct conversation {} between users {} and {}", 
                                existingConversation.getId(), currentUserId, otherParticipantId);
                        // Ensure both users are still participants (in case one was removed)
                        boolean currentUserIsParticipant = participantRepository
                                .findByConversationIdAndUserId(existingConversation.getId(), currentUserId)
                                .isPresent();
                        boolean otherUserIsParticipant = participantRepository
                                .findByConversationIdAndUserId(existingConversation.getId(), otherParticipantId)
                                .isPresent();
                        
                        // Re-add participants if they were removed
                        if (!currentUserIsParticipant) {
                            ConversationParticipant participant = new ConversationParticipant();
                            participant.setConversationId(existingConversation.getId());
                            participant.setUserId(currentUserId);
                            participantRepository.save(participant);
                            logger.info("Re-added current user {} as participant", currentUserId);
                        }
                        if (!otherUserIsParticipant) {
                            ConversationParticipant participant = new ConversationParticipant();
                            participant.setConversationId(existingConversation.getId());
                            participant.setUserId(otherParticipantId);
                            participantRepository.save(participant);
                            logger.info("Re-added other user {} as participant", otherParticipantId);
                        }
                        
                        return toConversationDto(existingConversation, currentUserId);
                    }
                }
            }
        }
        
        // No existing conversation found, create a new one
        logger.info("Creating new {} conversation", type);
        Conversation conversation = new Conversation();
        conversation.setType(type);
        conversation.setName(request.getName());
        UUID creatorId = request.getParticipantIds().isEmpty() 
                ? SecurityUtils.getCurrentUserId() 
                : request.getParticipantIds().get(0); // First participant is creator
        conversation.setCreatedBy(creatorId);
        
        conversation = conversationRepository.save(conversation);

        // Add participants
        for (UUID participantId : request.getParticipantIds()) {
            ConversationParticipant participant = new ConversationParticipant();
            participant.setConversationId(conversation.getId());
            participant.setUserId(participantId);
            participantRepository.save(participant);
        }

        // Get userId from request participants (first participant is creator)
        UUID userId = request.getParticipantIds().isEmpty() 
                ? SecurityUtils.getCurrentUserId() 
                : request.getParticipantIds().get(0);
        return toConversationDto(conversation, userId);
    }

    @Override
    public List<ConversationDto> getConversations(UUID userId, int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<Conversation> conversations = conversationRepository.findByParticipantUserId(userId, pageable);
        
        logger.info("Found {} conversations for userId={}, page={}, limit={}", 
                conversations.getContent().size(), userId, page, limit);
        
        List<ConversationDto> result = conversations.getContent().stream()
                .map(conv -> toConversationDto(conv, userId))
                .collect(Collectors.toList());
        
        logger.info("Returning {} conversation DTOs", result.size());
        return result;
    }

    @Override
    public ConversationDto getConversation(UUID conversationId, UUID userId) {
        return conversationRepository.findById(conversationId)
                .map(conv -> toConversationDto(conv, userId))
                .orElse(null);
    }

    @Override
    @Transactional(transactionManager = "messagingTransactionManager")
    public MessageDto sendMessage(CreateMessageRequest request) {
        logger.info("Sending message - conversationId: {}, senderId: {}, content length: {}", 
                request.getConversationId(), request.getSenderId(), 
                request.getContent() != null ? request.getContent().length() : 0);
        
        // Validate required fields
        if (request.getConversationId() == null) {
            throw new IllegalArgumentException("Conversation ID cannot be null");
        }
        if (request.getSenderId() == null) {
            throw new IllegalArgumentException("Sender ID cannot be null");
        }
        boolean hasText = request.getContent() != null && !request.getContent().trim().isEmpty();
        boolean hasAttachment = request.getAttachmentUrl() != null && !request.getAttachmentUrl().isBlank();
        if (!hasText && !hasAttachment) {
            throw new IllegalArgumentException("Message must have text content or an attachment URL");
        }
        
        // Verify conversation exists
        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Conversation not found: " + request.getConversationId()));
        
        // Verify sender is participant
        boolean isParticipant = participantRepository.findByConversationIdAndUserId(
                request.getConversationId(), request.getSenderId()).isPresent();
        
        if (!isParticipant) {
            logger.warn("User {} is not a participant in conversation {}. Checking if conversation creator...", 
                    request.getSenderId(), request.getConversationId());
            
            // If user is the creator, add them as participant (edge case fix)
            if (conversation.getCreatedBy() != null && conversation.getCreatedBy().equals(request.getSenderId())) {
                logger.info("User {} is conversation creator but not in participants. Adding as participant.", 
                        request.getSenderId());
                ConversationParticipant participant = new ConversationParticipant();
                participant.setConversationId(request.getConversationId());
                participant.setUserId(request.getSenderId());
                participantRepository.save(participant);
                logger.info("Added user {} as participant in conversation {}", 
                        request.getSenderId(), request.getConversationId());
            } else {
                throw new IllegalArgumentException(
                        String.format("User %s is not a participant in conversation %s", 
                                request.getSenderId(), request.getConversationId()));
            }
        }

        Message message = new Message();
        message.setConversationId(request.getConversationId());
        message.setSenderId(request.getSenderId());
        message.setContent(request.getContent() != null ? request.getContent() : "");
        message.setMessageType(request.getMessageType() != null ? request.getMessageType() : "TEXT");
        message.setAttachmentUrl(request.getAttachmentUrl());
        message.setAttachmentMetadata(request.getAttachmentMetadata());
        message.setReplyToId(request.getReplyToId());

        logger.debug("Saving message to database");
        message = messageRepository.save(message);
        logger.info("Message saved successfully with ID: {}", message.getId());

        // Update conversation last message (conversation already loaded above)
        conversation.setLastMessageId(message.getId());
        conversation.setLastMessageAt(message.getCreatedAt());
        conversationRepository.save(conversation);
        logger.debug("Updated conversation last message");

        // Publish dm.received event for notification service (async, non-blocking)
        // Use CompletableFuture to make it truly async and not block the response
        try {
            Message finalMessage = message;
            CompletableFuture.runAsync(() -> {
                try {
                    publishDmReceivedEvent(finalMessage, conversation);
                } catch (Exception e) {
                    logger.warn("Failed to publish dm.received event (async): {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            logger.warn("Failed to schedule dm.received event, but message was saved: {}", e.getMessage());
            // Don't fail the request if event publishing fails
        }

        MessageDto dto = toMessageDto(message);
        if (messageBroadcastService != null) {
            try {
                messageBroadcastService.broadcastPersistedMessage(dto);
            } catch (Exception e) {
                logger.warn("WebSocket fan-out failed (message still saved): {}", e.getMessage());
            }
        }
        return dto;
    }

    @Override
    public MessageDto getMessage(UUID conversationId, UUID messageId, UUID participantUserIdOrNullIfSystem) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        if (!message.getConversationId().equals(conversationId)) {
            throw new IllegalArgumentException("Message does not belong to this conversation");
        }
        if (participantUserIdOrNullIfSystem != null) {
            participantRepository.findByConversationIdAndUserId(conversationId, participantUserIdOrNullIfSystem)
                    .orElseThrow(() -> new IllegalArgumentException("User is not a participant"));
        }
        return toMessageDto(message);
    }

    @Override
    public void markMessageDelivered(UUID messageId, UUID userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        participantRepository.findByConversationIdAndUserId(message.getConversationId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not a participant"));
        if (message.getSenderId().equals(userId)) {
            return;
        }
        if (message.getStatus() == MessageStatus.SENT) {
            message.setStatus(MessageStatus.DELIVERED);
            messageRepository.save(message);
            logger.debug("Message {} marked DELIVERED for viewer {}", messageId, userId);
        }
    }

    @Override
    public void markMessageRead(UUID messageId, UUID userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        participantRepository.findByConversationIdAndUserId(message.getConversationId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not a participant"));
        if (message.getSenderId().equals(userId)) {
            return;
        }
        readReceiptRepository.findByMessageIdAndUserId(message.getId(), userId)
                .orElseGet(() -> {
                    MessageReadReceipt receipt = new MessageReadReceipt();
                    receipt.setMessageId(message.getId());
                    receipt.setUserId(userId);
                    return readReceiptRepository.save(receipt);
                });
        if (message.getStatus() != MessageStatus.READ) {
            message.setStatus(MessageStatus.READ);
            messageRepository.save(message);
        }
        logger.debug("Message {} marked READ for viewer {}", messageId, userId);
    }

    @Override
    public List<MessageDto> getMessages(UUID conversationId, UUID userId, int page, int limit) {
        // Verify user is participant
        participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not a participant"));

        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
        
        return messages.getContent().stream()
                .map(this::toMessageDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(transactionManager = "messagingTransactionManager")
    public void markAsRead(UUID conversationId, UUID userId) {
        // Mark all messages in conversation as read
        List<Message> messages = messageRepository.findByConversationIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                conversationId, PageRequest.of(0, 1000)).getContent();

        for (Message message : messages) {
            if (!message.getSenderId().equals(userId)) {
                readReceiptRepository.findByMessageIdAndUserId(message.getId(), userId)
                        .orElseGet(() -> {
                            MessageReadReceipt receipt = new MessageReadReceipt();
                            receipt.setMessageId(message.getId());
                            receipt.setUserId(userId);
                            return readReceiptRepository.save(receipt);
                        });
            }
        }

        // Update participant last read
        participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .ifPresent(participant -> {
                    participant.setLastReadAt(OffsetDateTime.now());
                    participantRepository.save(participant);
                });
    }

    @Override
    public ConversationDto getConversationInfo(UUID conversationId, UUID userId) {
        // Verify user is participant
        participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not a participant"));
        return getConversation(conversationId, userId);
    }

    @Override
    @Transactional(transactionManager = "messagingTransactionManager")
    public void deleteConversation(UUID conversationId, UUID userId) {
        // Verify user is participant
        participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not a participant"));
        
        // Soft delete: Remove user from participants
        participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .ifPresent(participantRepository::delete);
        
        // If no participants left, delete conversation
        if (participantRepository.findByConversationId(conversationId).isEmpty()) {
            conversationRepository.deleteById(conversationId);
        }
    }

    @Override
    @Transactional(transactionManager = "messagingTransactionManager")
    public void deleteMessage(UUID conversationId, UUID messageId, UUID userId) {
        // Verify user is participant
        participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not a participant"));
        
        // Verify message belongs to conversation and user is sender
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        
        if (!message.getConversationId().equals(conversationId)) {
            throw new IllegalArgumentException("Message does not belong to this conversation");
        }
        
        if (!message.getSenderId().equals(userId)) {
            throw new IllegalArgumentException("User is not the sender of this message");
        }
        
        // Soft delete
        message.setDeletedAt(OffsetDateTime.now());
        messageRepository.save(message);
    }

    @Override
    @Transactional(transactionManager = "messagingTransactionManager")
    public void addReaction(UUID conversationId, UUID messageId, UUID userId, String emoji) {
        // Verify user is participant
        participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not a participant"));
        
        // Verify message belongs to conversation
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        
        if (!message.getConversationId().equals(conversationId)) {
            throw new IllegalArgumentException("Message does not belong to this conversation");
        }
        
        // TODO: Implement reaction storage (would need a MessageReaction entity)
        logger.info("Adding reaction {} to message {} by user {}", emoji, messageId, userId);
    }

    @Override
    @Transactional(transactionManager = "messagingTransactionManager")
    public void removeReaction(UUID conversationId, UUID messageId, UUID userId, String emoji) {
        // Verify user is participant
        participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not a participant"));
        
        // Verify message belongs to conversation
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        
        if (!message.getConversationId().equals(conversationId)) {
            throw new IllegalArgumentException("Message does not belong to this conversation");
        }
        
        // TODO: Implement reaction removal (would need a MessageReaction entity)
        logger.info("Removing reaction {} from message {} by user {}", emoji, messageId, userId);
    }

    @Override
    public List<MessageDto> searchMessages(UUID conversationId, UUID userId, String query, int page, int limit) {
        // Verify user is participant
        participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not a participant"));
        
        // Simple content search (can be enhanced with full-text search)
        Pageable pageable = PageRequest.of(page - 1, limit);
        // Use a custom query or filter in memory if repository method doesn't exist
        Page<Message> allMessages = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
        List<Message> filteredMessages = allMessages.getContent().stream()
                .filter(m -> m.getDeletedAt() == null && 
                            m.getContent() != null && 
                            m.getContent().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
        
        return filteredMessages.stream()
                .map(this::toMessageDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatUserDto> searchUsers(String query, int page, int limit, UUID viewerId) {
        logger.info("Searching users with query: {}, page: {}, limit: {}, viewerId: {}", query, page, limit, viewerId);
        
        try {
            // Build URL for user-service search endpoint
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(userServiceUrl)
                    .path("/api/v1/users/search")
                    .queryParam("q", query)
                    .queryParam("page", page)
                    .queryParam("limit", limit);
            
            // Add viewer_id if available (for follow status)
            if (viewerId != null) {
                uriBuilder.queryParam("viewer_id", viewerId);
            }
            
            String url = uriBuilder.toUriString();
            
            logger.debug("Calling user-service at: {}", url);
            
            // Call user-service
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            List<Map<String, Object>> userDtos = response.getBody();
            if (userDtos == null || userDtos.isEmpty()) {
                logger.info("No users found for query: {}", query);
                return List.of();
            }
            
            logger.info("Found {} users for query: {}", userDtos.size(), query);
            
            // Convert UserDto (from user-service) to ChatUserDto
            return userDtos.stream()
                    .map(this::convertToChatUserDto)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            logger.error("Error searching users from user-service: {}", e.getMessage(), e);
            // Return empty list on error instead of throwing exception
            // This allows the UI to show "no results" instead of an error
            return List.of();
        }
    }
    
    private ChatUserDto convertToChatUserDto(Map<String, Object> userDto) {
        ChatUserDto chatUser = new ChatUserDto();
        
        // Extract ID
        Object idObj = userDto.get("id");
        if (idObj != null) {
            if (idObj instanceof String) {
                chatUser.setId(UUID.fromString((String) idObj));
            } else if (idObj instanceof UUID) {
                chatUser.setId((UUID) idObj);
            }
        }
        
        // Extract username
        Object usernameObj = userDto.get("username");
        if (usernameObj != null) {
            chatUser.setUsername(usernameObj.toString());
        }
        
        // Extract name
        Object nameObj = userDto.get("name");
        if (nameObj != null) {
            chatUser.setName(nameObj.toString());
        }
        
        // Extract profile picture URL
        Object profilePicObj = userDto.get("profile_picture_url");
        if (profilePicObj != null) {
            chatUser.setProfilePictureUrl(profilePicObj.toString());
        }
        
        // Extract is_verified
        Object verifiedObj = userDto.get("is_verified");
        if (verifiedObj != null) {
            if (verifiedObj instanceof Boolean) {
                chatUser.setIsVerified((Boolean) verifiedObj);
            } else if (verifiedObj instanceof String) {
                chatUser.setIsVerified(Boolean.parseBoolean((String) verifiedObj));
            }
        }
        
        // Extract is_following (from user-service when viewer_id is provided)
        Object followingObj = userDto.get("is_following");
        if (followingObj != null) {
            if (followingObj instanceof Boolean) {
                chatUser.setIsFollowing((Boolean) followingObj);
            } else if (followingObj instanceof String) {
                chatUser.setIsFollowing(Boolean.parseBoolean((String) followingObj));
            }
        }
        
        // Set is_online to false by default (user-service doesn't provide this)
        chatUser.setIsOnline(false);
        
        return chatUser;
    }

    private void publishDmReceivedEvent(Message message, Conversation conversation) {
        try {
            // Get recipients (all participants except sender)
            List<ConversationParticipant> participants = participantRepository.findByConversationId(conversation.getId());
            
            for (ConversationParticipant participant : participants) {
                if (!participant.getUserId().equals(message.getSenderId())) {
                    Map<String, Object> event = new HashMap<>();
                    UUID recipientId = participant.getUserId();
                    UUID senderId = message.getSenderId();
                    UUID conversationId = conversation.getId();
                    UUID messageId = message.getId();
                    event.put("recipientId", recipientId.toString());
                    event.put("senderId", senderId.toString());
                    event.put("conversationId", conversationId.toString());
                    event.put("messageId", messageId.toString());
                    String content = message.getContent();
                    String messagePreview = (content == null || content.isBlank())
                            ? ""
                            : (content.length() > 100 ? content.substring(0, 100) : content);
                    event.put("messagePreview", messagePreview);
                    
                    kafkaTemplate.send("dm.received", event);
                }
            }
        } catch (Exception e) {
            logger.error("Error publishing dm.received event: {}", e.getMessage());
        }
    }

    private ConversationDto toConversationDto(Conversation conversation, UUID currentUserId) {
        ConversationDto dto = new ConversationDto();
        dto.setId(conversation.getId());
        dto.setType(conversation.getType().name());
        dto.setName(conversation.getName());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setUpdatedAt(conversation.getUpdatedAt());

        // Load participants
        List<UUID> participants = participantRepository.findByConversationId(conversation.getId()).stream()
                .map(ConversationParticipant::getUserId)
                .collect(Collectors.toList());
        dto.setParticipants(participants);

        // For DIRECT conversations, get the other participant (not current user)
        // This is needed for Flutter to display participant info
        if (conversation.getType() == ConversationType.DIRECT && currentUserId != null) {
            UUID otherParticipantId = participants.stream()
                    .filter(id -> !id.equals(currentUserId))
                    .findFirst()
                    .orElse(null);
            
            if (otherParticipantId != null) {
                dto.setParticipantId(otherParticipantId);
                
                // Try to fetch participant details from user-service
                try {
                    String url = UriComponentsBuilder.fromHttpUrl(userServiceUrl)
                            .path("/api/v1/users/" + otherParticipantId)
                            .toUriString();
                    
                    logger.debug("Fetching user details from: {}", url);
                    ResponseEntity<Map<String, Object>> userResponse = restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<Map<String, Object>>() {}
                    );
                    
                    Map<String, Object> userData = userResponse.getBody();
                    if (userData != null) {
                        Object nameObj = userData.get("name");
                        Object usernameObj = userData.get("username");
                        Object avatarObj = userData.get("profile_picture_url");
                        
                        // Use name if available, fallback to username
                        String name = nameObj != null ? nameObj.toString() : 
                                     (usernameObj != null ? usernameObj.toString() : "Unknown");
                        String avatar = avatarObj != null ? avatarObj.toString() : "";
                        
                        dto.setParticipantName(name);
                        dto.setParticipantAvatarUrl(avatar);
                        dto.setIsOnline(false); // User-service doesn't provide online status yet
                        
                        logger.debug("Set participant info: name={}, avatar={}", name, avatar);
                    }
                } catch (Exception e) {
                    logger.warn("Could not fetch participant details from user-service: {}", e.getMessage());
                    // Set defaults if user-service is unavailable
                    dto.setParticipantName("Unknown");
                    dto.setParticipantAvatarUrl("");
                    dto.setIsOnline(false);
                }
            }
        } else if (conversation.getType() == ConversationType.GROUP) {
            // For group conversations, use conversation name
            dto.setParticipantName(conversation.getName() != null ? conversation.getName() : "Group Chat");
            dto.setParticipantAvatarUrl("");
            dto.setIsOnline(false);
        }

        // Load last message if exists
        if (conversation.getLastMessageId() != null) {
            messageRepository.findById(conversation.getLastMessageId())
                    .map(this::toMessageDto)
                    .ifPresent(dto::setLastMessage);
        }

        // Calculate unread count for this conversation
        Long unreadCount = calculateUnreadCount(conversation.getId(), currentUserId);
        dto.setUnreadCount(unreadCount);

        return dto;
    }
    
    /**
     * Calculate unread message count for a conversation
     */
    private Long calculateUnreadCount(UUID conversationId, UUID userId) {
        if (userId == null) {
            return 0L;
        }
        
        try {
            // Get participant's last read timestamp
            OffsetDateTime lastReadAt = participantRepository
                    .findByConversationIdAndUserId(conversationId, userId)
                    .map(ConversationParticipant::getLastReadAt)
                    .orElse(null);
            
            // Count messages after last read (or all messages if never read)
            // Messages that:
            // 1. Are not deleted
            // 2. Are not sent by the current user
            // 3. Were created after lastReadAt (or all if lastReadAt is null)
            // 4. Don't have a read receipt for this user
            
            List<Message> allMessages = messageRepository
                    .findByConversationIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                            conversationId, PageRequest.of(0, 10000))
                    .getContent();
            
            long unreadCount = 0;
            for (Message message : allMessages) {
                // Skip messages sent by the user
                if (message.getSenderId().equals(userId)) {
                    continue;
                }
                
                // If lastReadAt is null, count all messages
                // Otherwise, only count messages after lastReadAt
                if (lastReadAt == null || message.getCreatedAt().isAfter(lastReadAt)) {
                    // Check if there's a read receipt
                    boolean hasReadReceipt = readReceiptRepository
                            .findByMessageIdAndUserId(message.getId(), userId)
                            .isPresent();
                    
                    if (!hasReadReceipt) {
                        unreadCount++;
                    }
                }
            }
            
            return unreadCount;
        } catch (Exception e) {
            logger.warn("Error calculating unread count for conversation {}: {}", conversationId, e.getMessage());
            return 0L;
        }
    }
    
    @Override
    public Long getTotalUnreadCount(UUID userId) {
        if (userId == null) {
            return 0L;
        }
        
        try {
            // Get all conversations where user is a participant
            List<ConversationParticipant> participants = participantRepository.findByUserId(userId);
            
            long totalUnread = 0;
            for (ConversationParticipant participant : participants) {
                Long unreadCount = calculateUnreadCount(participant.getConversationId(), userId);
                totalUnread += unreadCount;
            }
            
            return totalUnread;
        } catch (Exception e) {
            logger.error("Error calculating total unread count for user {}: {}", userId, e.getMessage(), e);
            return 0L;
        }
    }

    private MessageDto toMessageDto(Message message) {
        MessageDto dto = new MessageDto();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversationId());
        dto.setSenderId(message.getSenderId());
        dto.setContent(message.getContent());
        dto.setMessageType(message.getMessageType());
        dto.setAttachmentUrl(message.getAttachmentUrl());
        dto.setAttachmentMetadata(message.getAttachmentMetadata());
        dto.setReplyToId(message.getReplyToId());
        dto.setStatus(message.getStatus() != null ? message.getStatus().name() : null);
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }
}

