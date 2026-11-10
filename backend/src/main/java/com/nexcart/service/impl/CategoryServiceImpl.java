package com.nexcart.service.impl;

import com.nexcart.dto.request.CategoryRequest;
import com.nexcart.dto.response.CategoryResponse;
import com.nexcart.dto.response.PagedResponse;
import com.nexcart.entity.Category;
import com.nexcart.exception.CategoryAlreadyExistsException;
import com.nexcart.exception.CategoryNotFoundException;
import com.nexcart.mapper.CategoryMapper;
import com.nexcart.repository.CategoryRepository;
import com.nexcart.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;


@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @CacheEvict(cacheNames = "categories", allEntries = true)
    public CategoryResponse createCategory(CategoryRequest request) {

        if (categoryRepository.existsByName(request.getName())) {
            throw new CategoryAlreadyExistsException(
                    "Category already exists.");
        }

        Category category = categoryMapper.toEntity(request);

        Category savedCategory = categoryRepository.save(category);

        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    @Cacheable(cacheNames = "categories", key = "'page-v2:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort.toString()")
    public PagedResponse<CategoryResponse> getAllCategories(Pageable pageable) {
        return PagedResponse.from(categoryRepository.findAll(pageable).map(categoryMapper::toResponse));
    }

    @Override
    public CategoryResponse getCategoryById(Long id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new CategoryNotFoundException("Category not found"));

        return categoryMapper.toResponse(category);
    }

    @Override
    @CacheEvict(cacheNames = "categories", allEntries = true)
    public CategoryResponse updateCategory(Long id,
                                           CategoryRequest request) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Category not found"));

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());

        Category updated = categoryRepository.save(category);

        return categoryMapper.toResponse(updated);
    }

    @Override
    @CacheEvict(cacheNames = "categories", allEntries = true)
    public void deleteCategory(Long id) {

        if (!categoryRepository.existsById(id)) {
            throw new CategoryNotFoundException("Category not found");
        }

        categoryRepository.deleteById(id);
    }
}

