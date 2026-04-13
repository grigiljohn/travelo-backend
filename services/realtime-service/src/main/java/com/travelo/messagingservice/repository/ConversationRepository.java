package com.travelo.messagingservice.repository;

import com.travelo.messagingservice.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    
    @Query("SELECT c FROM Conversation c " +
           "JOIN ConversationParticipant cp ON c.id = cp.conversationId " +
           "WHERE cp.userId = :userId AND cp.leftAt IS NULL " +
           "ORDER BY c.lastMessageAt DESC NULLS LAST, c.updatedAt DESC")
    Page<Conversation> findByParticipantUserId(@Param("userId") UUID userId, Pageable pageable);
    
    @Query("SELECT c FROM Conversation c " +
           "JOIN ConversationParticipant cp1 ON c.id = cp1.conversationId " +
           "JOIN ConversationParticipant cp2 ON c.id = cp2.conversationId " +
           "WHERE c.type = 'DIRECT' " +
           "AND cp1.userId = :userId1 AND cp2.userId = :userId2 " +
           "AND cp1.leftAt IS NULL AND cp2.leftAt IS NULL")
    Conversation findDirectConversation(@Param("userId1") UUID userId1, @Param("userId2") UUID userId2);
}

