package com.travelo.postservice.controller;

import com.travelo.postservice.dto.ApiResponse;
import com.travelo.postservice.dto.CreatePollRequest;
import com.travelo.postservice.dto.PollDto;
import com.travelo.postservice.service.PollService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/polls")
public class PollController {
    private static final Logger logger = LoggerFactory.getLogger(PollController.class);
    private final PollService pollService;

    public PollController(PollService pollService) {
        this.pollService = pollService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PollDto>> createPoll(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePollRequest request) {
        try {
            String userId = jwt.getSubject();
            logger.info("Creating poll for post {} by user {}", request.postId(), userId);
            PollDto poll = pollService.createPoll(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Poll created successfully", poll));
        } catch (Exception e) {
            logger.error("Error creating poll", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to create poll: " + e.getMessage(), "POLL_CREATE_FAILED"));
        }
    }

    @GetMapping("/{pollId}")
    public ResponseEntity<ApiResponse<PollDto>> getPoll(@PathVariable String pollId) {
        try {
            PollDto poll = pollService.getPollById(pollId);
            return ResponseEntity.ok(ApiResponse.success("Poll fetched successfully", poll));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage(), "POLL_NOT_FOUND"));
        } catch (Exception e) {
            logger.error("Error fetching poll", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch poll: " + e.getMessage(), "POLL_FETCH_FAILED"));
        }
    }

    @PostMapping("/{pollId}/vote")
    public ResponseEntity<ApiResponse<PollDto>> voteOnPoll(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String pollId,
            @RequestParam Integer optionIndex) {
        try {
            String userId = jwt.getSubject();
            logger.info("User {} voting on poll {} for option {}", userId, pollId, optionIndex);
            PollDto poll = pollService.voteOnPoll(pollId, userId, optionIndex);
            return ResponseEntity.ok(ApiResponse.success("Vote recorded successfully", poll));
        } catch (Exception e) {
            logger.error("Error voting on poll", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to vote: " + e.getMessage(), "POLL_VOTE_FAILED"));
        }
    }
}

