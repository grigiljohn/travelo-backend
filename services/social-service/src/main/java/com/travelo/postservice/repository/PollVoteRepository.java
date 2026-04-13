package com.travelo.postservice.repository;

import com.travelo.postservice.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PollVoteRepository extends JpaRepository<PollVote, String> {
    Optional<PollVote> findByPollIdAndUserId(String pollId, String userId);
    
    List<PollVote> findByPollId(String pollId);
    
    long countByPollId(String pollId);
}

