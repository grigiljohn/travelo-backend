package com.travelo.searchservice.repository;

import com.travelo.searchservice.document.PostDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostDocumentRepository extends ElasticsearchRepository<PostDocument, String> {
    
    Page<PostDocument> findByCaptionContaining(String caption, Pageable pageable);
    
    Page<PostDocument> findByLocationContaining(String location, Pageable pageable);
    
    Page<PostDocument> findByTagsContaining(String tag, Pageable pageable);
    
    Page<PostDocument> findByPostType(String postType, Pageable pageable);
}

