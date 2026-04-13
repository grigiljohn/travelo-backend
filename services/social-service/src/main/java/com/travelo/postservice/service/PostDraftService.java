package com.travelo.postservice.service;

import com.travelo.postservice.dto.CreateDraftRequest;
import com.travelo.postservice.dto.PostDraftDto;

import java.util.List;
import java.util.UUID;

public interface PostDraftService {
    PostDraftDto createDraft(String userId, CreateDraftRequest request);
    List<PostDraftDto> getUserDrafts(String userId);
    PostDraftDto getDraftById(UUID draftId, String userId);
    void deleteDraft(UUID draftId, String userId);
    PostDraftDto scheduleDraft(UUID draftId, String userId, java.time.OffsetDateTime scheduledAt);
}

