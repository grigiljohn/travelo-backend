package com.travelo.mediaservice.repository;

import com.travelo.mediaservice.entity.MediaFile;
import com.travelo.mediaservice.entity.MediaStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaFileRepository extends JpaRepository<MediaFile, UUID> {

    Optional<MediaFile> findByStorageKey(String storageKey);

    List<MediaFile> findByOwnerId(UUID ownerId);

    long countByState(MediaStatus state);

    List<MediaFile> findByState(MediaStatus state);
}

