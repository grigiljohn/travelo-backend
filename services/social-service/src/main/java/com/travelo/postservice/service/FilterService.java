package com.travelo.postservice.service;

import com.travelo.postservice.dto.FilterDto;

import java.util.List;

public interface FilterService {
    List<FilterDto> getAllFilters();
    
    List<FilterDto> getFiltersByType(String type);
}

