package com.nexcart.controller;

import com.nexcart.dto.request.AddressRequest;
import com.nexcart.dto.response.AddressResponse;
import com.nexcart.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@Tag(name = "Address Management", description = "APIs for managing user addresses")
@SecurityRequirement(name = "Bearer Authentication")
public class AddressController {

    private final AddressService addressService;

    @Operation(summary = "Add a new address")
    @PostMapping
    public ResponseEntity<AddressResponse> addAddress(
            @Valid @RequestBody AddressRequest request) {

        AddressResponse response = addressService.addAddress(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get all addresses of the logged-in user")
    @GetMapping
    public ResponseEntity<List<AddressResponse>> getMyAddresses() {

        return ResponseEntity.ok(addressService.getMyAddresses());
    }

    @Operation(summary = "Get address by ID")
    @GetMapping("/{addressId}")
    public ResponseEntity<AddressResponse> getAddressById(
            @PathVariable Long addressId) {

        return ResponseEntity.ok(addressService.getAddressById(addressId));
    }

    @Operation(summary = "Update an existing address")
    @PutMapping("/{addressId}")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request) {

        return ResponseEntity.ok(
                addressService.updateAddress(addressId, request)
        );
    }

    @Operation(summary = "Delete an address")
    @DeleteMapping("/{addressId}")
    public ResponseEntity<String> deleteAddress(
            @PathVariable Long addressId) {

        addressService.deleteAddress(addressId);
        return ResponseEntity.ok("Address deleted successfully.");
    }

    @Operation(summary = "Set an address as the default address")
    @PutMapping("/{addressId}/default")
    public ResponseEntity<String> setDefaultAddress(
            @PathVariable Long addressId) {

        addressService.setDefaultAddress(addressId);
        return ResponseEntity.ok("Default address updated successfully.");
    }
}
