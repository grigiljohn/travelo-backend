package com.travelo.circlesservice.repository;

import com.travelo.circlesservice.persistence.CircleCommunityMemberEntity;
import com.travelo.circlesservice.persistence.MemberPk;
import com.travelo.circlesservice.persistence.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CircleCommunityMemberRepository extends JpaRepository<CircleCommunityMemberEntity, MemberPk> {

    boolean existsByCommunityIdAndUserId(String communityId, String userId);

    boolean existsByCommunityIdAndUserIdAndMembershipStatus(
            String communityId,
            String userId,
            MembershipStatus membershipStatus
    );

    @Query("""
            select distinct m.communityId from CircleCommunityMemberEntity m
            where m.userId = :userId and m.communityId in :ids
              and m.membershipStatus = MembershipStatus.ACTIVE
            """)
    List<String> findCommunityIdsContainingUser(
            @Param("userId") String userId,
            @Param("ids") Collection<String> communityIds
    );

    @Query("""
            select m.userId from CircleCommunityMemberEntity m
            where m.communityId = :communityId and m.membershipStatus = MembershipStatus.PENDING
            order by m.joinedAt asc
            """)
    List<String> findPendingUserIds(@Param("communityId") String communityId);
}
