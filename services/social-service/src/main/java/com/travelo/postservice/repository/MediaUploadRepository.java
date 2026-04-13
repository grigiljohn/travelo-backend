package com.travelo.postservice.repository;

import com.travelo.postservice.entity.MediaUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaUploadRepository extends JpaRepository<MediaUpload, String> {
    List<MediaUpload> findByUserIdOrderByCreatedAtDesc(String userId);
}

