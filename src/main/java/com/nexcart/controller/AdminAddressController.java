package com.nexcart.controller;

import com.nexcart.dto.response.AddressResponse;
import com.nexcart.service.AdminAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/addresses")
@RequiredArgsConstructor
@Tag(
        name = "Admin Address",
        description = "Admin Address Management APIs"
)
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAddressController {

    private final AdminAddressService adminAddressService;

    @GetMapping
    @Operation(summary = "Get All Addresses")
    public ResponseEntity<List<AddressResponse>> getAllAddresses() {

        return ResponseEntity.ok(
                adminAddressService.getAllAddresses());
    }

    @GetMapping("/{addressId}")
    @Operation(summary = "Get Address By ID")
    public ResponseEntity<AddressResponse> getAddressById(
            @PathVariable Long addressId) {

        return ResponseEntity.ok(
                adminAddressService.getAddressById(addressId));
    }

    @DeleteMapping("/{addressId}")
    @Operation(summary = "Delete Address")
    public ResponseEntity<String> deleteAddress(
            @PathVariable Long addressId) {

        adminAddressService.deleteAddress(addressId);

        return ResponseEntity.ok(
                "Address deleted successfully.");
    }
}
