package com.travelo.postservice.service;

import com.travelo.postservice.dto.CreatePollRequest;
import com.travelo.postservice.dto.PollDto;

public interface PollService {
    PollDto createPoll(String userId, CreatePollRequest request);
    
    PollDto getPollById(String pollId);
    
    PollDto voteOnPoll(String pollId, String userId, Integer optionIndex);
}

