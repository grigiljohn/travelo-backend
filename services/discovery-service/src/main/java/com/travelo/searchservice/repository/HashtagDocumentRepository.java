package com.travelo.searchservice.repository;

import com.travelo.searchservice.document.HashtagDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HashtagDocumentRepository extends ElasticsearchRepository<HashtagDocument, String> {
    
    Optional<HashtagDocument> findByTag(String tag);
    
    Page<HashtagDocument> findByNameContaining(String name, Pageable pageable);
}

