package com.travelo.planservice.repository;

import com.travelo.planservice.persistence.CirclePlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CirclePlanRepository extends JpaRepository<CirclePlanEntity, UUID> {

    /** Newest first; capped by Spring Data {@code TopN} for feed injection. */
    List<CirclePlanEntity> findTop48ByOrderByCreatedAtDesc();
}
