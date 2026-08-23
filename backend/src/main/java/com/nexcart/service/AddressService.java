package com.nexcart.service;

import com.nexcart.dto.request.AddressRequest;
import com.nexcart.dto.response.AddressResponse;

import java.util.List;

public interface AddressService {

    AddressResponse addAddress(AddressRequest request);

    List<AddressResponse> getMyAddresses();

    AddressResponse getAddressById(Long addressId);

    AddressResponse updateAddress(Long addressId, AddressRequest request);

    void deleteAddress(Long addressId);

    void setDefaultAddress(Long addressId);
}
