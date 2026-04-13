package com.travelo.postservice.controller;

import com.travelo.postservice.dto.PostTemplateDto;
import com.travelo.postservice.service.PostTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/templates")
@Tag(name = "Post Templates", description = "Post template management APIs")
public class PostTemplateController {

    private static final Logger logger = LoggerFactory.getLogger(PostTemplateController.class);
    private final PostTemplateService templateService;

    public PostTemplateController(PostTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    @Operation(summary = "Get all active templates", description = "Returns all active post templates ordered by display order")
    public ResponseEntity<List<PostTemplateDto>> getAllTemplates() {
        logger.info("GET /api/v1/templates");
        List<PostTemplateDto> templates = templateService.getAllTemplates();
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{templateId}")
    @Operation(summary = "Get template by ID", description = "Returns a specific template by its ID")
    public ResponseEntity<PostTemplateDto> getTemplateById(@PathVariable String templateId) {
        logger.info("GET /api/v1/templates/{}", templateId);
        PostTemplateDto template = templateService.getTemplateById(templateId);
        return ResponseEntity.ok(template);
    }
}

