package com.travelo.admin.repository;

import com.travelo.admin.domain.FeatureFlag;
import com.travelo.admin.domain.FeaturePlatform;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, Long> {
    Optional<FeatureFlag> findByFeatureNameIgnoreCaseAndPlatform(String featureName, FeaturePlatform platform);

    List<FeatureFlag> findAllByOrderByFeatureNameAsc();
}
