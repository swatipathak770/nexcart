package com.nexcart.service;

import com.nexcart.dto.request.BrandRequest;
import com.nexcart.dto.response.BrandResponse;
import com.nexcart.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface BrandService {

    BrandResponse createBrand(BrandRequest request);

    PagedResponse<BrandResponse> getAllBrands(Pageable pageable);

    BrandResponse getBrandById(Long id);

    BrandResponse updateBrand(Long id, BrandRequest request);

    void deleteBrand(Long id);
}
