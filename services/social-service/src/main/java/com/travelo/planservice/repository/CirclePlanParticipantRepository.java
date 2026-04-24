package com.travelo.planservice.repository;

import com.travelo.planservice.persistence.CirclePlanParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CirclePlanParticipantRepository extends JpaRepository<CirclePlanParticipantEntity, Long> {

    List<CirclePlanParticipantEntity> findByPlanIdOrderByJoinedAtAsc(UUID planId);

    List<CirclePlanParticipantEntity> findByPlanIdIn(Collection<UUID> planIds);
}
