package mexa.club.shopservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mexa.club.shopservice.client.payload.ApiResponse;
import mexa.club.shopservice.dto.AddressResponse;
import mexa.club.shopservice.dto.CreateAddressRequest;
import mexa.club.shopservice.dto.UpdateAddressRequest;
import mexa.club.shopservice.security.ShopUsers;
import mexa.club.shopservice.service.CustomerAddressService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "My Addresses", description = "Delivery address book management for the authenticated customer")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/shops/me/addresses")
@org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
public class MeAddressController {

    private final CustomerAddressService customerAddressService;

    public MeAddressController(CustomerAddressService customerAddressService) {
        this.customerAddressService = customerAddressService;
    }

    @Operation(
        summary = "List my delivery addresses",
        description = "Returns all saved delivery addresses for the authenticated user. The default address is indicated in the response."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Address list retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping
    public ApiResponse<List<AddressResponse>> list(Authentication authentication) {
        UUID userId = ShopUsers.requireUserId(authentication);
        return ApiResponse.ok(customerAddressService.list(userId));
    }

    @Operation(
        summary = "Add a new delivery address",
        description = "Creates a new delivery address for the authenticated user. If this is the first address it will be set as default automatically."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Address created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid address data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PostMapping
    public ApiResponse<AddressResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateAddressRequest body
    ) {
        UUID userId = ShopUsers.requireUserId(authentication);
        return ApiResponse.ok(customerAddressService.create(userId, body));
    }

    @Operation(
        summary = "Update a delivery address",
        description = "Replaces all fields of the specified delivery address with the provided values."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Address updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid address data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Address not found")
    })
    @PutMapping("/{id}")
    public ApiResponse<AddressResponse> update(
            Authentication authentication,
            @Parameter(description = "UUID of the address to update", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAddressRequest body
    ) {
        UUID userId = ShopUsers.requireUserId(authentication);
        return ApiResponse.ok(customerAddressService.update(userId, id, body));
    }

    @Operation(
        summary = "Delete a delivery address",
        description = "Permanently removes the specified delivery address from the authenticated user's address book."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Address deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Address not found")
    })
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            Authentication authentication,
            @Parameter(description = "UUID of the address to delete", required = true)
            @PathVariable UUID id) {
        UUID userId = ShopUsers.requireUserId(authentication);
        customerAddressService.delete(userId, id);
        return ApiResponse.okVoid();
    }

    @Operation(
        summary = "Set address as default",
        description = "Marks the specified address as the default delivery address. Any previously default address is unset."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Default address updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Address not found")
    })
    @PatchMapping("/{id}/default")
    public ApiResponse<AddressResponse> makeDefault(
            Authentication authentication,
            @Parameter(description = "UUID of the address to set as default", required = true)
            @PathVariable UUID id) {
        UUID userId = ShopUsers.requireUserId(authentication);
        return ApiResponse.ok(customerAddressService.setDefault(userId, id));
    }
}
