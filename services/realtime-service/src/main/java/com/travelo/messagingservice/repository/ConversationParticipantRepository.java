package com.travelo.messagingservice.repository;

import com.travelo.messagingservice.entity.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {
    
    List<ConversationParticipant> findByConversationId(UUID conversationId);
    
    Optional<ConversationParticipant> findByConversationIdAndUserId(UUID conversationId, UUID userId);
    
    List<ConversationParticipant> findByUserIdAndLeftAtIsNull(UUID userId);
    
    List<ConversationParticipant> findByUserId(UUID userId);
}

