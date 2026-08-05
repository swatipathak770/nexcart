package com.nexcart.service.impl;

import com.nexcart.dto.response.AddressResponse;
import com.nexcart.entity.Address;
import com.nexcart.exception.ResourceNotFoundException;
import com.nexcart.mapper.AddressMapper;
import com.nexcart.repository.AddressRepository;
import com.nexcart.service.AdminAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminAddressServiceImpl implements AdminAddressService {

    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;

    @Override
    public List<AddressResponse> getAllAddresses() {

        return addressRepository.findAll()
                .stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    @Override
    public AddressResponse getAddressById(Long addressId) {

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Address not found."));

        return addressMapper.toResponse(address);
    }

    @Override
    public void deleteAddress(Long addressId) {

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Address not found."));

        addressRepository.delete(address);
    }
}
