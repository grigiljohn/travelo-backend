package com.travelo.circlesservice.repository;

import com.travelo.circlesservice.persistence.CircleCommunityTagEntity;
import com.travelo.circlesservice.persistence.CommunityTagPk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CircleCommunityTagRepository extends JpaRepository<CircleCommunityTagEntity, CommunityTagPk> {

    List<CircleCommunityTagEntity> findByCommunityIdInOrderByTagAsc(Collection<String> communityIds);

    void deleteByCommunityId(String communityId);
}
