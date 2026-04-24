package com.travelo.admin.api.catalog;

import com.travelo.admin.repository.AdminCategoryRepository;
import com.travelo.admin.repository.AdminTagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PublicCatalogService {
    private final AdminCategoryRepository categoryRepository;
    private final AdminTagRepository tagRepository;

    public PublicCatalogService(AdminCategoryRepository categoryRepository, AdminTagRepository tagRepository) {
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public List<PublicCategoryDto> listActiveCategories() {
        return categoryRepository.findByActiveIsTrueOrderByNameAsc().stream()
                .map(c -> new PublicCategoryDto(
                        c.getId(),
                        c.getName(),
                        c.getIcon() == null ? "" : c.getIcon()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicTagDto> listActiveTags() {
        return tagRepository.findByActiveIsTrueOrderByNameAsc().stream()
                .map(t -> new PublicTagDto(
                        t.getId(),
                        t.getName(),
                        t.getSlug()
                ))
                .toList();
    }
}
