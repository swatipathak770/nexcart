package com.nexcart.service;

import com.nexcart.dto.request.BrandRequest;
import com.nexcart.dto.response.BrandResponse;

import java.util.List;

public interface BrandService {

    BrandResponse createBrand(BrandRequest request);

    List<BrandResponse> getAllBrands();

    BrandResponse getBrandById(Long id);

    BrandResponse updateBrand(Long id, BrandRequest request);

    void deleteBrand(Long id);
}
