package com.travelo.postservice.service.impl;

import com.travelo.postservice.dto.CreatePollRequest;
import com.travelo.postservice.dto.PollDto;
import com.travelo.postservice.entity.Poll;
import com.travelo.postservice.entity.PollVote;
import com.travelo.postservice.repository.PollRepository;
import com.travelo.postservice.repository.PollVoteRepository;
import com.travelo.postservice.service.PollService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PollServiceImpl implements PollService {
    private static final Logger logger = LoggerFactory.getLogger(PollServiceImpl.class);
    
    private final PollRepository pollRepository;
    private final PollVoteRepository pollVoteRepository;
    
    public PollServiceImpl(PollRepository pollRepository, PollVoteRepository pollVoteRepository) {
        this.pollRepository = pollRepository;
        this.pollVoteRepository = pollVoteRepository;
    }
    
    @Override
    @Transactional
    public PollDto createPoll(String userId, CreatePollRequest request) {
        Poll poll = Poll.builder()
            .postId(request.postId())
            .question(request.question())
            .options(request.options())
            .expiresAt(request.expiresAt())
            .build();
        
        poll = pollRepository.save(poll);
        return toDto(poll);
    }
    
    @Override
    public PollDto getPollById(String pollId) {
        Poll poll = pollRepository.findById(pollId)
            .orElseThrow(() -> new RuntimeException("Poll not found: " + pollId));
        return toDto(poll);
    }
    
    @Override
    @Transactional
    public PollDto voteOnPoll(String pollId, String userId, Integer optionIndex) {
        Poll poll = pollRepository.findById(pollId)
            .orElseThrow(() -> new RuntimeException("Poll not found: " + pollId));
        
        // Check if user already voted
        pollVoteRepository.findByPollIdAndUserId(pollId, userId)
            .ifPresent(vote -> {
                throw new RuntimeException("User already voted on this poll");
            });
        
        // Validate option index
        if (optionIndex < 0 || optionIndex >= poll.getOptions().size()) {
            throw new RuntimeException("Invalid option index: " + optionIndex);
        }
        
        // Create vote
        PollVote vote = PollVote.builder()
            .poll(poll)
            .userId(userId)
            .optionIndex(optionIndex)
            .build();
        pollVoteRepository.save(vote);
        
        // Update poll vote count
        poll.setTotalVotes(poll.getTotalVotes() + 1);
        pollRepository.save(poll);
        
        return toDto(poll);
    }
    
    private PollDto toDto(Poll poll) {
        // Calculate vote counts per option
        List<PollVote> votes = pollVoteRepository.findByPollId(poll.getId());
        Map<Integer, Long> voteCounts = votes.stream()
            .collect(Collectors.groupingBy(PollVote::getOptionIndex, Collectors.counting()));
        
        return new PollDto(
            poll.getId(),
            poll.getPostId(),
            poll.getQuestion(),
            poll.getOptions(),
            poll.getTotalVotes(),
            voteCounts,
            poll.getExpiresAt(),
            poll.getCreatedAt()
        );
    }
}

