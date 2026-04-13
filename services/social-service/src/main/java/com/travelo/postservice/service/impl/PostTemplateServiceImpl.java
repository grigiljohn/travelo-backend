package com.travelo.postservice.service.impl;

import com.travelo.postservice.dto.PostTemplateDto;
import com.travelo.postservice.entity.PostTemplate;
import com.travelo.postservice.repository.PostTemplateRepository;
import com.travelo.postservice.service.PostTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PostTemplateServiceImpl implements PostTemplateService {

    private static final Logger log = LoggerFactory.getLogger(PostTemplateServiceImpl.class);

    private final PostTemplateRepository repository;

    public PostTemplateServiceImpl(PostTemplateRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<PostTemplateDto> getAllTemplates() {
        log.debug("Fetching all active templates");
        List<PostTemplate> templates = repository.findByIsActiveTrueOrderByDisplayOrderAsc();
        return templates.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public PostTemplateDto getTemplateById(String templateId) {
        log.debug("Fetching template by ID: {}", templateId);
        return repository.findByTemplateId(templateId)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
    }

    private PostTemplateDto toDto(PostTemplate template) {
        return new PostTemplateDto(
                template.getTemplateId(),
                template.getLabel(),
                template.getIcon(),
                template.getThumbnailUrl(),
                template.getIsDefault(),
                template.getDisplayOrder()
        );
    }
}

