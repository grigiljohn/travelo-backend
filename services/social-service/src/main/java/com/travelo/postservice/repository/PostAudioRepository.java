package com.travelo.postservice.repository;

import com.travelo.postservice.entity.PostAudio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostAudioRepository extends JpaRepository<PostAudio, String> {
    Optional<PostAudio> findByPostIdAndAudioId(String postId, String audioId);
    
    List<PostAudio> findByPostId(String postId);
}

