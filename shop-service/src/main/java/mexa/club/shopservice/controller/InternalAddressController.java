package mexa.club.shopservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mexa.club.shopservice.dto.InternalAddressResponse;
import mexa.club.shopservice.entity.CustomerAddress;
import mexa.club.shopservice.repository.CustomerAddressRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Tag(name = "Internal — Addresses", description = "Internal service-to-service endpoint for resolving customer address details (region, district, phone) when creating deliveries. Protected by a shared API key header (X-Internal-Api-Key), not by JWT.")
@RestController
@RequestMapping("/api/shops/internal/addresses")
public class InternalAddressController {

    private final CustomerAddressRepository addressRepository;
    private final String internalApiKey;

    public InternalAddressController(
            CustomerAddressRepository addressRepository,
            @Value("${app.internal-api.key:change-me-internal}") String internalApiKey
    ) {
        this.addressRepository = addressRepository;
        this.internalApiKey = internalApiKey;
    }

    @Operation(
            summary = "Get address details by ID (internal)",
            description = "Returns the full address record (region, district, phone, address lines) for the specified address UUID. Called by order-service when creating a delivery after an order is confirmed. Authentication is performed via the X-Internal-Api-Key header rather than a JWT Bearer token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address returned successfully"),
            @ApiResponse(responseCode = "403", description = "Missing or invalid X-Internal-Api-Key header"),
            @ApiResponse(responseCode = "404", description = "Address not found")
    })
    @GetMapping("/{id}")
    public InternalAddressResponse getById(
            @Parameter(description = "UUID of the address to resolve", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Internal API key for service-to-service authentication", required = true)
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String providedKey
    ) {
        if (providedKey == null || !providedKey.equals(internalApiKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal API key");
        }
        CustomerAddress a = addressRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
        return new InternalAddressResponse(
                a.getId(),
                a.getUserId(),
                a.getLabel(),
                a.getName(),
                a.getLatitude(),
                a.getLongitude(),
                a.getLine2(),
                a.getPhone(),
                a.isDefaultAddress()
        );
    }
}
