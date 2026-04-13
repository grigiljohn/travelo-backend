package com.travelo.searchservice.repository;

import com.travelo.searchservice.document.ShopDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShopDocumentRepository extends ElasticsearchRepository<ShopDocument, String> {
    
    Page<ShopDocument> findByNameContainingOrDescriptionContaining(String name, String description, Pageable pageable);
}

