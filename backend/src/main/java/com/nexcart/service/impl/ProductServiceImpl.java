package com.nexcart.service.impl;

import com.nexcart.dto.request.ProductRequest;
import com.nexcart.dto.response.ProductResponse;
import com.nexcart.dto.response.PagedResponse;
import com.nexcart.entity.Brand;
import com.nexcart.entity.Category;
import com.nexcart.entity.Product;
import com.nexcart.exception.*;
import com.nexcart.mapper.ProductMapper;
import com.nexcart.repository.BrandRepository;
import com.nexcart.repository.CategoryRepository;
import com.nexcart.repository.ProductRepository;
import com.nexcart.service.ProductService;
import com.nexcart.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductMapper productMapper;

    @Override
    @CacheEvict(cacheNames = "products", allEntries = true)
    public ProductResponse createProduct(ProductRequest request) {

        if (productRepository.existsBySku(request.getSku())) {
            throw new ProductAlreadyExistsException("SKU already exists.");
        }

        Category category = getCategory(request.getCategoryId());
        Brand brand = getBrand(request.getBrandId());

        Product product = productMapper.toEntity(request, category, brand);

        Product savedProduct = productRepository.save(product);

        savedProduct = productRepository.findById(savedProduct.getId())
                .orElseThrow(() ->
                        new ProductNotFoundException("Product not found."));

        return productMapper.toResponse(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "products", key = "'page-v2:all:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort.toString()")
    public PagedResponse<ProductResponse> getAllProducts(Pageable pageable) {
        return PagedResponse.from(productRepository.findAll(pageable).map(productMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productDetails", key = "#id")
    public ProductResponse getProductById(Long id) {

        Product product = getProduct(id);

        return productMapper.toResponse(product);
    }

    @Override
    @CacheEvict(cacheNames = {"products", "productDetails"}, allEntries = true)
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

        updatedProduct = productRepository.findById(updatedProduct.getId())
                .orElseThrow(() ->
                        new ProductNotFoundException("Product not found."));

        return productMapper.toResponse(updatedProduct);
    }

    @Override
    @CacheEvict(cacheNames = {"products", "productDetails"}, allEntries = true)
    public void deleteProduct(Long id) {

        Product product = getProduct(id);

        productRepository.delete(product);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "products", key = "'page-v2:search:' + #keyword + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort.toString()")
    public PagedResponse<ProductResponse> searchProducts(String keyword, Pageable pageable) {
        return PagedResponse.from(productRepository.findByNameContainingIgnoreCase(keyword, pageable)
                .map(productMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "products", key = "'page-v2:filter:' + #keyword + ':' + #category + ':' + #brand + ':' + #minPrice + ':' + #maxPrice + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort.toString()")
    public PagedResponse<ProductResponse> filterProducts(
            String keyword,
            String category,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {

        Specification<Product> specification =
                ProductSpecification.hasKeyword(keyword)
                        .and(ProductSpecification.hasCategory(category))
                        .and(ProductSpecification.hasBrand(brand))
                        .and(ProductSpecification.hasMinPrice(minPrice))
                        .and(ProductSpecification.hasMaxPrice(maxPrice));

        return PagedResponse.from(productRepository.findAll(specification, pageable)
                .map(productMapper::toResponse));
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
}
