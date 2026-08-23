package com.nexcart.mapper;

import com.nexcart.dto.request.BrandRequest;
import com.nexcart.dto.response.BrandResponse;
import com.nexcart.entity.Brand;
import org.springframework.stereotype.Component;

@Component
public class BrandMapper {

    public Brand toEntity(BrandRequest request) {
        return Brand.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    public BrandResponse toResponse(Brand brand) {
        return BrandResponse.builder()
                .id(brand.getId())
                .name(brand.getName())
                .description(brand.getDescription())
                .build();
    }
}
