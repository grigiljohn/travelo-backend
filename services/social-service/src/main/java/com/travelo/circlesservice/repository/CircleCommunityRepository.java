package com.travelo.circlesservice.repository;

import com.travelo.circlesservice.persistence.CircleCommunityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CircleCommunityRepository extends JpaRepository<CircleCommunityEntity, String> {

    @Query("""
            select distinct c from CircleCommunityEntity c
            where lower(trim(c.visibility)) = 'public'
               or exists (select 1 from CircleCommunityMemberEntity m where m.communityId = c.id and m.userId = :userId)
            """)
    List<CircleCommunityEntity> findAccessibleForUser(@Param("userId") String userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = "UPDATE circle_communities SET member_count = member_count + 1, last_activity_label = :label, updated_at = NOW() WHERE id = :id",
            nativeQuery = true
    )
    void incrementMemberCount(@Param("id") String id, @Param("label") String label);
}
