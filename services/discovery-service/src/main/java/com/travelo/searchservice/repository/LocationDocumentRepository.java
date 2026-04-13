package com.travelo.searchservice.repository;

import com.travelo.searchservice.document.LocationDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationDocumentRepository extends ElasticsearchRepository<LocationDocument, String> {
    
    Page<LocationDocument> findByNameContainingOrCityContainingOrCountryContaining(
            String name, String city, String country, Pageable pageable);
}

