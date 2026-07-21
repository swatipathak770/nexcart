package com.nexcart.service.impl;

import com.nexcart.dto.request.ProductRequest;
import com.nexcart.dto.response.ProductResponse;
import com.nexcart.entity.Brand;
import com.nexcart.entity.Category;
import com.nexcart.entity.Product;
import com.nexcart.exception.*;
import com.nexcart.mapper.ProductMapper;
import com.nexcart.repository.BrandRepository;
import com.nexcart.repository.CategoryRepository;
import com.nexcart.repository.ProductRepository;
import com.nexcart.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.nexcart.specification.ProductSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;


import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductMapper productMapper;

    @Override
    public ProductResponse createProduct(ProductRequest request) {

        if (productRepository.existsBySku(request.getSku())) {
            throw new ProductAlreadyExistsException("SKU already exists.");
        }

        Category category = getCategory(request.getCategoryId());
        Brand brand = getBrand(request.getBrandId());

        Product product = productMapper.toEntity(request, category, brand);

        Product savedProduct = productRepository.save(product);

        return productMapper.toResponse(savedProduct);
    }

    @Override
    public Page<ProductResponse> getAllProducts(
            int page,
            int size,
            String sortBy,
            String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return productRepository.findAll(pageable)
                .map(productMapper::toResponse);
    }
    @Override
    public ProductResponse getProductById(Long id) {

        Product product = getProduct(id);

        return productMapper.toResponse(product);
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {

        Product product = getProduct(id);

        if (productRepository.existsBySku(request.getSku())
                && !product.getSku().equals(request.getSku())) {
            throw new ProductAlreadyExistsException("SKU already exists.");
        }

        Category category = getCategory(request.getCategoryId());
        Brand brand = getBrand(request.getBrandId());

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setSku(request.getSku());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);
        product.setBrand(brand);

        Product updatedProduct = productRepository.save(product);

        return productMapper.toResponse(updatedProduct);
    }

    @Override
    public void deleteProduct(Long id) {

        Product product = getProduct(id);

        productRepository.delete(product);
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException("Product not found."));
    }

    private Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new CategoryNotFoundException("Category not found."));
    }

    private Brand getBrand(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() ->
                        new BrandNotFoundException("Brand not found."));
    }
    @Override
    public List<ProductResponse> searchProducts(String keyword) {

        return productRepository
                .findByNameContainingIgnoreCase(keyword)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> filterProducts(
            String keyword,
            String category,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Product> specification =
                ProductSpecification.hasKeyword(keyword)
                        .and(ProductSpecification.hasCategory(category))
                        .and(ProductSpecification.hasBrand(brand))
                        .and(ProductSpecification.hasMinPrice(minPrice))
                        .and(ProductSpecification.hasMaxPrice(maxPrice));

        return productRepository.findAll(specification, pageable)
                .map(productMapper::toResponse);
    }
}
