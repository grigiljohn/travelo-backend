package com.travelo.postservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelo.postservice.dto.CreateDraftRequest;
import com.travelo.postservice.dto.PostDraftDto;
import com.travelo.postservice.entity.PostDraft;
import com.travelo.postservice.repository.PostDraftRepository;
import com.travelo.postservice.service.PostDraftService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PostDraftServiceImpl implements PostDraftService {

    private static final Logger logger = LoggerFactory.getLogger(PostDraftServiceImpl.class);
    private final PostDraftRepository draftRepository;
    private final ObjectMapper objectMapper;

    public PostDraftServiceImpl(PostDraftRepository draftRepository, ObjectMapper objectMapper) {
        this.draftRepository = draftRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public PostDraftDto createDraft(String userId, CreateDraftRequest request) {
        logger.info("Creating draft for user: {}", userId);

        PostDraft draft = new PostDraft();
        draft.setUserId(userId);
        draft.setMediaFilePath(request.mediaFilePath());
        draft.setIsVideo(request.isVideo() != null ? request.isVideo() : false);
        draft.setTitle(request.title());
        draft.setCaption(request.caption());
        draft.setText(request.text());
        draft.setLocation(request.location());
        draft.setAudience(request.audience());
        draft.setAllowComments(request.allowComments() != null ? request.allowComments() : true);
        draft.setHideLikesCount(request.hideLikesCount() != null ? request.hideLikesCount() : false);
        draft.setAllowRemixing(request.allowRemixing() != null ? request.allowRemixing() : true);
        draft.setAiLabelEnabled(request.aiLabelEnabled() != null ? request.aiLabelEnabled() : false);
        draft.setMusicTrackId(request.musicTrackId());
        draft.setFilter(request.filter());
        draft.setCreateMode(request.createMode());
        draft.setCoverImagePath(request.coverImagePath());
        draft.setScheduledAt(request.scheduledAt());

        // Convert lists to JSON strings
        if (request.hashtags() != null && !request.hashtags().isEmpty()) {
            try {
                draft.setHashtags(objectMapper.writeValueAsString(request.hashtags()));
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize hashtags", e);
            }
        }
        if (request.taggedUsers() != null && !request.taggedUsers().isEmpty()) {
            try {
                draft.setTaggedUsers(objectMapper.writeValueAsString(request.taggedUsers()));
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize tagged users", e);
            }
        }

        PostDraft savedDraft = draftRepository.save(draft);
        logger.info("Draft created successfully with ID: {}", savedDraft.getId());

        return toDto(savedDraft);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostDraftDto> getUserDrafts(String userId) {
        logger.info("Fetching drafts for user: {}", userId);
        return draftRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PostDraftDto getDraftById(UUID draftId, String userId) {
        logger.info("Fetching draft {} for user: {}", draftId, userId);
        PostDraft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new RuntimeException("Draft not found: " + draftId));

        if (!draft.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to draft: " + draftId);
        }

        return toDto(draft);
    }

    @Override
    @Transactional
    public void deleteDraft(UUID draftId, String userId) {
        logger.info("Deleting draft {} for user: {}", draftId, userId);
        PostDraft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new RuntimeException("Draft not found: " + draftId));

        if (!draft.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to draft: " + draftId);
        }

        draftRepository.delete(draft);
        logger.info("Draft deleted successfully: {}", draftId);
    }

    @Override
    @Transactional
    public PostDraftDto scheduleDraft(UUID draftId, String userId, java.time.OffsetDateTime scheduledAt) {
        logger.info("Scheduling draft {} for user: {} at {}", draftId, userId, scheduledAt);
        PostDraft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new RuntimeException("Draft not found: " + draftId));

        if (!draft.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to draft: " + draftId);
        }

        draft.setScheduledAt(scheduledAt);
        PostDraft updatedDraft = draftRepository.save(draft);
        logger.info("Draft scheduled successfully: {} at {}", draftId, scheduledAt);

        return toDto(updatedDraft);
    }

    private PostDraftDto toDto(PostDraft draft) {
        List<String> hashtags = null;
        List<String> taggedUsers = null;

        try {
            if (draft.getHashtags() != null && !draft.getHashtags().isEmpty()) {
                hashtags = objectMapper.readValue(draft.getHashtags(), 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            }
            if (draft.getTaggedUsers() != null && !draft.getTaggedUsers().isEmpty()) {
                taggedUsers = objectMapper.readValue(draft.getTaggedUsers(), 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize JSON fields", e);
        }

        return new PostDraftDto(
                draft.getId(),
                draft.getUserId(),
                draft.getMediaFilePath(),
                draft.getIsVideo(),
                draft.getTitle(),
                draft.getCaption(),
                draft.getText(),
                hashtags,
                draft.getLocation(),
                taggedUsers,
                draft.getAudience(),
                draft.getAllowComments(),
                draft.getHideLikesCount(),
                draft.getAllowRemixing(),
                draft.getAiLabelEnabled(),
                draft.getMusicTrackId(),
                draft.getFilter(),
                draft.getCreateMode(),
                draft.getCoverImagePath(),
                draft.getScheduledAt(),
                draft.getCreatedAt(),
                draft.getUpdatedAt()
        );
    }
}

