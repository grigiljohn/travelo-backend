package com.travelo.postservice.service.impl;

import com.travelo.postservice.dto.FilterDto;
import com.travelo.postservice.entity.Filter;
import com.travelo.postservice.repository.FilterRepository;
import com.travelo.postservice.service.FilterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FilterServiceImpl implements FilterService {
    private static final Logger logger = LoggerFactory.getLogger(FilterServiceImpl.class);
    
    private final FilterRepository filterRepository;
    
    public FilterServiceImpl(FilterRepository filterRepository) {
        this.filterRepository = filterRepository;
    }
    
    @Override
    public List<FilterDto> getAllFilters() {
        return filterRepository.findByIsActiveTrueOrderByDisplayOrder().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<FilterDto> getFiltersByType(String type) {
        return filterRepository.findByTypeAndIsActiveTrueOrderByDisplayOrder(type).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    private FilterDto toDto(Filter filter) {
        return new FilterDto(
            filter.getId(),
            filter.getName(),
            filter.getType(),
            filter.getPreviewUrl(),
            filter.getConfig(),
            filter.getDisplayOrder()
        );
    }
}

