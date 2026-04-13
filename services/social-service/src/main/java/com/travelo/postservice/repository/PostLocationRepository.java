package com.travelo.postservice.repository;

import com.travelo.postservice.entity.PostLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostLocationRepository extends JpaRepository<PostLocation, String> {
    Optional<PostLocation> findByPostIdAndLocationId(String postId, String locationId);
    
    List<PostLocation> findByPostId(String postId);
}

