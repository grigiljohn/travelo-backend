package com.travelo.collectionservice.repository;

import com.travelo.collectionservice.entity.Collection;
import com.travelo.collectionservice.entity.CollectionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CollectionRepository extends JpaRepository<Collection, UUID> {
    Page<Collection> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    Page<Collection> findByUserIdAndTypeOrderByCreatedAtDesc(String userId, CollectionType type, Pageable pageable);
    Optional<Collection> findByIdAndUserId(UUID id, String userId);
    Optional<Collection> findByUserIdAndTypeAndTripId(String userId, CollectionType type, String tripId);
}
