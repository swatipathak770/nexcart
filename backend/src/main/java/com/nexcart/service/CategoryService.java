package com.nexcart.service;

import com.nexcart.dto.request.CategoryRequest;
import com.nexcart.dto.response.CategoryResponse;
import com.nexcart.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface CategoryService {

    CategoryResponse createCategory(CategoryRequest request);

    PagedResponse<CategoryResponse> getAllCategories(Pageable pageable);

    CategoryResponse getCategoryById(Long id);

    CategoryResponse updateCategory(Long id, CategoryRequest request);

    void deleteCategory(Long id);
}
