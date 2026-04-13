package com.travelo.momentsservice.controller;

import com.travelo.commons.security.SecurityUtils;
import com.travelo.momentsservice.dto.CreateMomentCommentRequest;
import com.travelo.momentsservice.dto.MomentAiSuggestionResponse;
import com.travelo.momentsservice.dto.MomentCommentResponse;
import com.travelo.momentsservice.dto.MomentCreateResponse;
import com.travelo.momentsservice.dto.MomentDetailsResponse;
import com.travelo.momentsservice.dto.MomentFeedItemResponse;
import com.travelo.momentsservice.dto.MomentLikeResponse;
import com.travelo.momentsservice.service.MomentsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/moments")
public class MomentsController {

    private static final Logger logger = LoggerFactory.getLogger(MomentsController.class);
    private final MomentsService momentsService;
    private final boolean devHeaderUserFallbackEnabled;

    public MomentsController(
            MomentsService momentsService,
            @Value("${app.dev.allow-header-user-fallback:true}") boolean devHeaderUserFallbackEnabled
    ) {
        this.momentsService = momentsService;
        this.devHeaderUserFallbackEnabled = devHeaderUserFallbackEnabled;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MomentCreateResponse> createMoment(
            @RequestParam("type") @NotBlank String type,
            @RequestParam(value = "mediaType", defaultValue = "image") String mediaType,
            @RequestParam(value = "caption", defaultValue = "") String caption,
            @RequestParam(value = "location", defaultValue = "") String location,
            @RequestParam(value = "tags", defaultValue = "") String tags,
            @RequestParam(value = "mediaUrls", defaultValue = "") String mediaUrls,
            @RequestParam(value = "thumbnailPath", defaultValue = "") String thumbnailPath,
            @RequestParam(value = "trimStart", required = false) Double trimStart,
            @RequestParam(value = "trimEnd", required = false) Double trimEnd,
            @RequestParam(value = "videoFilter", defaultValue = "") String videoFilter,
            @RequestParam(value = "cropPreset", defaultValue = "") String cropPreset,
            @RequestParam(value = "musicUrl", defaultValue = "") String musicUrl,
            @RequestParam(value = "musicName", defaultValue = "") String musicName,
            @RequestParam(value = "musicStart", required = false) Double musicStart,
            @RequestParam(value = "aiEnhanced", defaultValue = "false") boolean aiEnhanced,
            @RequestParam(value = "segmentsJson", defaultValue = "") String segmentsJson,
            @RequestParam(value = "highlightsJson", defaultValue = "") String highlightsJson,
            @RequestParam(value = "scenesJson", defaultValue = "") String scenesJson,
            @RequestParam(value = "mediaDurationsJson", defaultValue = "[]") String mediaDurationsJson,
            @RequestParam(value = "editorMetadataJson", defaultValue = "") String editorMetadataJson,
            @RequestParam(value = "audience", defaultValue = "followers") String audience,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Name", required = false) String headerUserName,
            @RequestParam(value = "files", required = false) List<MultipartFile> files
    ) throws IOException {
        final String effectiveUserId = resolveUserId(headerUserId);
        final String effectiveUserName = resolveUserName(headerUserName);
        final List<String> parsedTags = Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        final List<String> parsedMediaUrls = Arrays.stream(mediaUrls.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        logger.info(
                "flow=moment_create POST /api/v1/moments type={} mediaType={} mediaUrlCount={} tagCount={} "
                        + "captionLen={} aiEnhanced={} multipartFileCount={} trimStart={} trimEnd={}",
                type,
                mediaType,
                parsedMediaUrls.size(),
                parsedTags.size(),
                caption != null ? caption.length() : 0,
                aiEnhanced,
                files == null ? 0 : files.size(),
                trimStart,
                trimEnd);
        if (logger.isDebugEnabled() && !parsedMediaUrls.isEmpty()) {
            logger.debug("flow=moment_create mediaUrls(first3)={}",
                    parsedMediaUrls.stream().limit(3).toList());
        }
        final MomentCreateResponse response = momentsService.createMoment(
                effectiveUserId,
                effectiveUserName,
                type,
                mediaType,
                caption,
                location,
                parsedTags,
                parsedMediaUrls,
                thumbnailPath,
                trimStart,
                trimEnd,
                videoFilter,
                cropPreset,
                musicUrl,
                musicName,
                musicStart,
                aiEnhanced,
                segmentsJson,
                highlightsJson,
                scenesJson,
                mediaDurationsJson,
                editorMetadataJson,
                audience,
                files
        );
        logger.info("flow=moment_create OK momentId={} storedCount={}",
                response.id(), response.storedFiles() != null ? response.storedFiles().size() : 0);
        return ResponseEntity.ok(response);
    }

    /**
     * Static path must be declared before <code>/{momentId}</code> so <code>/feed</code> is not captured as an id.
     */
    @GetMapping("/feed")
    public ResponseEntity<List<MomentFeedItemResponse>> getFeed(
            @RequestParam(defaultValue = "20") int limit,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId
    ) {
        logger.info("GET /api/v1/moments/feed?limit={}", limit);
        final String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/moments")
                .toUriString();
        final String viewer = resolveUserId(headerUserId);
        return ResponseEntity.ok(momentsService.getFeed(baseUrl, limit, viewer));
    }

    @PostMapping("/ai/suggest")
    public ResponseEntity<MomentAiSuggestionResponse> suggestAi(
            @RequestParam(value = "action", defaultValue = "caption") String action,
            @RequestParam(value = "caption", defaultValue = "") String caption,
            @RequestParam(value = "location", defaultValue = "") String location,
            @RequestParam(value = "tags", defaultValue = "") String tags
    ) {
        logger.info("POST /api/v1/moments/ai/suggest - action={}", action);
        return ResponseEntity.ok(momentsService.suggestAiEdits(action, caption, location, tags));
    }

    @GetMapping("/{momentId}")
    public ResponseEntity<MomentDetailsResponse> getMoment(@PathVariable String momentId) throws IOException {
        logger.info("GET /api/v1/moments/{}", momentId);
        return ResponseEntity.ok(momentsService.getMoment(momentId));
    }

    @PostMapping("/{momentId}/like")
    public ResponseEntity<MomentLikeResponse> toggleMomentLike(
            @PathVariable String momentId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId
    ) {
        final String userId = resolveUserId(headerUserId);
        if (userId.equals("current-user")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required to like moments");
        }
        return ResponseEntity.ok(momentsService.toggleMomentLike(momentId, userId));
    }

    @PostMapping("/{momentId}/view")
    public ResponseEntity<Void> recordMomentView(
            @PathVariable String momentId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId
    ) {
        final String userId = resolveUserId(headerUserId);
        if (!userId.equals("current-user")) {
            momentsService.recordMomentView(momentId, userId);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{momentId}/comments")
    public ResponseEntity<List<MomentCommentResponse>> listMomentComments(
            @PathVariable String momentId
    ) {
        return ResponseEntity.ok(momentsService.listMomentComments(momentId));
    }

    @PostMapping("/{momentId}/comments")
    public ResponseEntity<MomentCommentResponse> addMomentComment(
            @PathVariable String momentId,
            @Valid @RequestBody CreateMomentCommentRequest body,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Name", required = false) String headerUserName
    ) {
        final String userId = resolveUserId(headerUserId);
        if (userId.equals("current-user")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required to comment");
        }
        final String userName = resolveUserName(headerUserName);
        final MomentCommentResponse created = momentsService.addMomentComment(
                momentId,
                userId,
                userName,
                body.commentText()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{momentId}/files/{fileName}")
    public ResponseEntity<Resource> getMomentFile(
            @PathVariable String momentId,
            @PathVariable String fileName
    ) throws IOException {
        final Resource resource = momentsService.getMomentFile(momentId, fileName);
        final String contentType = Files.probeContentType(Path.of(resource.getFile().getAbsolutePath()));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType))
                .body(resource);
    }

    private String resolveUserId(String headerUserId) {
        final String jwtUserId = SecurityUtils.getCurrentUserIdAsString();
        if (jwtUserId != null && !jwtUserId.isBlank()) {
            return jwtUserId;
        }
        if (devHeaderUserFallbackEnabled && headerUserId != null && !headerUserId.isBlank()) {
            return headerUserId.trim();
        }
        return "current-user";
    }

    private String resolveUserName(String headerUserName) {
        if (headerUserName != null && !headerUserName.isBlank()) {
            return headerUserName.trim();
        }
        return "You";
    }
}
