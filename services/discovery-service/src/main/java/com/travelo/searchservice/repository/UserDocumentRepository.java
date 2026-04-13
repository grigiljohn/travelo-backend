package com.travelo.searchservice.repository;

import com.travelo.searchservice.document.UserDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDocumentRepository extends ElasticsearchRepository<UserDocument, String> {
    
    Page<UserDocument> findByUsernameContainingOrDisplayNameContaining(String username, String displayName, Pageable pageable);
    
    Page<UserDocument> findByBioContaining(String bio, Pageable pageable);
}

