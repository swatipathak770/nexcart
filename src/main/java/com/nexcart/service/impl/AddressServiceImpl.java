package com.nexcart.service.impl;

import com.nexcart.dto.request.AddressRequest;
import com.nexcart.dto.response.AddressResponse;
import com.nexcart.entity.Address;
import com.nexcart.entity.User;
import com.nexcart.mapper.AddressMapper;
import com.nexcart.repository.AddressRepository;
import com.nexcart.repository.UserRepository;
import com.nexcart.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;

    @Override
    public AddressResponse addAddress(AddressRequest request) {

        User user = getCurrentUser();

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.findByUserAndIsDefaultTrue(user)
                    .ifPresent(address -> {
                        address.setIsDefault(false);
                        addressRepository.save(address);
                    });
        }

        Address address = addressMapper.toEntity(request);
        address.setUser(user);

        Address savedAddress = addressRepository.save(address);

        return addressMapper.toResponse(savedAddress);
    }

    @Override
    public List<AddressResponse> getMyAddresses() {

        User user = getCurrentUser();

        return addressRepository.findByUser(user)
                .stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    @Override
    public AddressResponse getAddressById(Long addressId) {

        User user = getCurrentUser();

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found."));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You are not authorized to access this address.");
        }

        return addressMapper.toResponse(address);
    }

    @Override
    public AddressResponse updateAddress(Long addressId, AddressRequest request) {

        User user = getCurrentUser();

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found."));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You are not authorized to update this address.");
        }

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.findByUserAndIsDefaultTrue(user)
                    .ifPresent(defaultAddress -> {
                        defaultAddress.setIsDefault(false);
                        addressRepository.save(defaultAddress);
                    });
        }

        addressMapper.updateEntity(address, request);

        Address updatedAddress = addressRepository.save(address);

        return addressMapper.toResponse(updatedAddress);
    }

    @Override
    public void deleteAddress(Long addressId) {

        User user = getCurrentUser();

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found."));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You are not authorized to delete this address.");
        }

        addressRepository.delete(address);
    }

    @Override
    public void setDefaultAddress(Long addressId) {

        User user = getCurrentUser();

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found."));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You are not authorized to update this address.");
        }

        addressRepository.findByUserAndIsDefaultTrue(user)
                .ifPresent(defaultAddress -> {
                    defaultAddress.setIsDefault(false);
                    addressRepository.save(defaultAddress);
                });

        address.setIsDefault(true);
        addressRepository.save(address);
    }

    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found."));
    }
}
