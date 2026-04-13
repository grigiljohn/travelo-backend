package com.travelo.postservice.repository;

import com.travelo.postservice.entity.PostDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostDraftRepository extends JpaRepository<PostDraft, UUID> {
    List<PostDraft> findByUserIdOrderByUpdatedAtDesc(String userId);
    void deleteByUserId(String userId);
}

