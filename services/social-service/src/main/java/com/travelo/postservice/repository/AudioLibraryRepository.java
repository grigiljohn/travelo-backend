package com.travelo.postservice.repository;

import com.travelo.postservice.entity.AudioLibrary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AudioLibraryRepository extends JpaRepository<AudioLibrary, String> {
    List<AudioLibrary> findByIsActiveTrue();
    
    List<AudioLibrary> findByCategoryAndIsActiveTrue(String category);
    
    List<AudioLibrary> findByIsActiveTrueOrderByName();
}

