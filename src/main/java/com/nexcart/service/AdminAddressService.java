package com.nexcart.service;

import com.nexcart.dto.response.AddressResponse;

import java.util.List;

public interface AdminAddressService {

    List<AddressResponse> getAllAddresses();

    AddressResponse getAddressById(Long addressId);

    void deleteAddress(Long addressId);
}
