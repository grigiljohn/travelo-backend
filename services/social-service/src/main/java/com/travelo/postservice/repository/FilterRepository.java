package com.travelo.postservice.repository;

import com.travelo.postservice.entity.Filter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FilterRepository extends JpaRepository<Filter, String> {
    List<Filter> findByTypeAndIsActiveTrueOrderByDisplayOrder(String type);
    
    List<Filter> findByIsActiveTrueOrderByDisplayOrder();
}

