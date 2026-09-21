package mexa.club.shopservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mexa.club.shopservice.client.payload.ApiResponse;
import mexa.club.shopservice.dto.CoverageZoneResponse;
import mexa.club.shopservice.dto.GeoSearchResult;
import mexa.club.shopservice.service.GeoCoverageService;
import mexa.club.shopservice.service.GeoSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Geo Search", description = "Address search for checkout/address book: text query returns candidates with coordinates")
@RestController
@RequestMapping("/api/shops/geo")
public class GeoSearchController {

    private final GeoSearchService geoSearchService;
    private final GeoCoverageService geoCoverageService;

    public GeoSearchController(GeoSearchService geoSearchService, GeoCoverageService geoCoverageService) {
        this.geoSearchService = geoSearchService;
        this.geoCoverageService = geoCoverageService;
    }

    /**
     * Manzil qidiruv: matn → nomzodlar (name + lat + lng).
     * GET /api/shops/geo/search?query=Yunusobod
     */
    @Operation(
        summary = "Search addresses",
        description = "Returns up to 5 address candidates with names and coordinates for the given text query."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Candidate list returned successfully (may be empty)")
    })
    @GetMapping("/search")
    public ApiResponse<List<GeoSearchResult>> search(
            @Parameter(description = "Address text to search", required = true)
            @RequestParam String query
    ) {
        return ApiResponse.ok(geoSearchService.search(query));
    }

    /**
     * Teskari geokodlash: xaritadan tanlangan nuqta → manzil nomi.
     * GET /api/shops/geo/reverse?lat=...&lng=...
     */
    @Operation(
        summary = "Reverse geocode a point",
        description = "Returns the address name for the given coordinates (picked on the map)."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Address name returned successfully (may be null)")
    })
    @GetMapping("/reverse")
    public ApiResponse<GeoSearchService.ReverseResult> reverse(
            @Parameter(description = "Latitude", required = true)
            @RequestParam BigDecimal lat,
            @Parameter(description = "Longitude", required = true)
            @RequestParam BigDecimal lng
    ) {
        return ApiResponse.ok(geoSearchService.reverse(lat, lng));
    }

    /**
     * Xizmat hududlari: faol geo-poligonlar (mobil xaritada chiziladi,
     * foydalanuvchi faqat shu hududlar ichidan manzil tanlay oladi).
     * GET /api/shops/geo/coverage
     */
    @Operation(
        summary = "Service coverage zones",
        description = "Returns active delivery coverage polygons for the mobile map picker."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Coverage list returned successfully (may be empty)")
    })
    @GetMapping("/coverage")
    public ApiResponse<List<CoverageZoneResponse>> coverage() {
        return ApiResponse.ok(geoCoverageService.coverage());
    }

    /**
     * Nuqta xizmat hududidami: lat/lng → served + zona nomi.
     * GET /api/shops/geo/check?lat=...&lng=...
     */
    @Operation(
        summary = "Check point coverage",
        description = "Returns whether the given coordinates are inside a service zone."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Coverage status returned successfully")
    })
    @GetMapping("/check")
    public ApiResponse<mexa.club.shopservice.dto.CoverageCheckResponse> check(
            @Parameter(description = "Latitude", required = true)
            @RequestParam double lat,
            @Parameter(description = "Longitude", required = true)
            @RequestParam double lng
    ) {
        return ApiResponse.ok(geoCoverageService.check(lat, lng));
    }
}
