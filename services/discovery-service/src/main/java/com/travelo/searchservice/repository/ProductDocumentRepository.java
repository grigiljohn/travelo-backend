package com.travelo.searchservice.repository;

import com.travelo.searchservice.document.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductDocumentRepository extends ElasticsearchRepository<ProductDocument, String> {
    
    Page<ProductDocument> findByNameContainingOrDescriptionContaining(String name, String description, Pageable pageable);
}

