package com.nexcart.service.impl;

import com.nexcart.dto.request.BrandRequest;
import com.nexcart.dto.response.BrandResponse;
import com.nexcart.entity.Brand;
import com.nexcart.exception.BrandAlreadyExistsException;
import com.nexcart.exception.BrandNotFoundException;
import com.nexcart.mapper.BrandMapper;
import com.nexcart.repository.BrandRepository;
import com.nexcart.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    @Override
    public BrandResponse createBrand(BrandRequest request) {

        if (brandRepository.existsByName(request.getName())) {
            throw new BrandAlreadyExistsException("Brand already exists.");
        }

        Brand brand = brandMapper.toEntity(request);

        Brand savedBrand = brandRepository.save(brand);

        return brandMapper.toResponse(savedBrand);
    }

    @Override
    public List<BrandResponse> getAllBrands() {

        return brandRepository.findAll()
                .stream()
                .map(brandMapper::toResponse)
                .toList();
    }

    @Override
    public BrandResponse getBrandById(Long id) {

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() ->
                        new BrandNotFoundException("Brand not found"));

        return brandMapper.toResponse(brand);
    }

    @Override
    public BrandResponse updateBrand(Long id, BrandRequest request) {

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() ->
                        new BrandNotFoundException("Brand not found"));

        if (brandRepository.existsByName(request.getName())
                && !brand.getName().equalsIgnoreCase(request.getName())) {
            throw new BrandAlreadyExistsException("Brand already exists.");
        }

        brand.setName(request.getName());
        brand.setDescription(request.getDescription());

        Brand updatedBrand = brandRepository.save(brand);

        return brandMapper.toResponse(updatedBrand);
    }

    @Override
    public void deleteBrand(Long id) {

        if (!brandRepository.existsById(id)) {
            throw new BrandNotFoundException("Brand not found");
        }

        brandRepository.deleteById(id);
    }
}
