package com.travelo.postservice.service;

import com.travelo.postservice.dto.PostTemplateDto;

import java.util.List;

public interface PostTemplateService {
    
    /**
     * Get all active templates ordered by display order
     */
    List<PostTemplateDto> getAllTemplates();
    
    /**
     * Get template by ID
     */
    PostTemplateDto getTemplateById(String templateId);
}

