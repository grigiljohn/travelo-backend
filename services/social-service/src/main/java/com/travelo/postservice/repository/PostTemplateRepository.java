package com.travelo.postservice.repository;

import com.travelo.postservice.entity.PostTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostTemplateRepository extends JpaRepository<PostTemplate, UUID> {
    
    List<PostTemplate> findByIsActiveTrueOrderByDisplayOrderAsc();
    
    Optional<PostTemplate> findByTemplateId(String templateId);
    
    Optional<PostTemplate> findByIsDefaultTrueAndIsActiveTrue();
}

