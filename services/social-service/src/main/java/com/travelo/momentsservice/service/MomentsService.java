package com.travelo.momentsservice.service;

import com.travelo.momentsservice.dto.MomentAiSuggestionResponse;
import com.travelo.momentsservice.dto.MomentCommentResponse;
import com.travelo.momentsservice.dto.MomentCreateResponse;
import com.travelo.momentsservice.dto.MomentDetailsResponse;
import com.travelo.momentsservice.dto.MomentFeedItemResponse;
import com.travelo.momentsservice.dto.MomentLikeResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface MomentsService {
    MomentCreateResponse createMoment(
            String userId,
            String userName,
            String type,
            String mediaType,
            String caption,
            String location,
            List<String> tags,
            List<String> mediaUrls,
            String thumbnailPath,
            Double trimStart,
            Double trimEnd,
            String videoFilter,
            String cropPreset,
            String musicUrl,
            String musicName,
            Double musicStart,
            boolean aiEnhanced,
            String segmentsJson,
            String highlightsJson,
            String scenesJson,
            String mediaDurationsJson,
            String editorMetadataJson,
            String audience,
            List<MultipartFile> files
    ) throws IOException;

    MomentDetailsResponse getMoment(String momentId) throws IOException;

    List<MomentFeedItemResponse> getFeed(String baseUrl, int limit, String viewerUserId);

    MomentLikeResponse toggleMomentLike(String momentId, String userId);

    void recordMomentView(String momentId, String userId);

    List<MomentCommentResponse> listMomentComments(String momentId);

    MomentCommentResponse addMomentComment(
            String momentId,
            String userId,
            String userName,
            String commentText
    );

    Resource getMomentFile(String momentId, String fileName) throws IOException;

    MomentAiSuggestionResponse suggestAiEdits(
            String action,
            String caption,
            String location,
            String tags
    );
}
