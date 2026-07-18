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
    public List<ProductResponse> getAllProducts() {

        return productRepository.findAll()
                .stream()
                .map(productMapper::toResponse)
                .toList();
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
}
