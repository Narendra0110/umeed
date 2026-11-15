package com.umeed.categories;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listActiveCategories() {
        return categoryRepository.findByActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    /**
     * Tells other modules whether a category exists and is active.
     */
    @Transactional(readOnly = true)
    public boolean isActiveCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .map(Category::isActive)
                .orElse(false);
    }
}
