package mexa.club.shopservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mexa.club.shopservice.client.payload.ApiResponse;
import mexa.club.shopservice.dto.HomeResponse;
import mexa.club.shopservice.security.RequestTokenProvider;
import mexa.club.shopservice.service.HomeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Home", description = "Bosh sahifa uchun bitta bundle endpoint — 4 round-trip o'rniga 1 ta")
@RestController
@RequestMapping("/api/shops")
public class HomeController {

    private final HomeService homeService;
    private final RequestTokenProvider requestTokenProvider;

    public HomeController(HomeService homeService, RequestTokenProvider requestTokenProvider) {
        this.homeService = homeService;
        this.requestTokenProvider = requestTokenProvider;
    }

    /**
     * Bosh sahifa bundle: trending (20) + recommended (10, chegirmalilar) + brands + categories.
     * Har bir bo'lim mustaqil yuklanadi — biri xato bersa qolganlari baribir qaytadi.
     * GET /api/shops/home
     */
    @Operation(
        summary = "Homepage bundle",
        description = "Returns trending products, discounted recommendations, brands and categories in a single response."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Homepage bundle returned successfully")
    })
    @GetMapping("/home")
    public ApiResponse<HomeResponse> home() {
        // Token so'rov thread'ida olinadi (async thread'larda RequestContextHolder ishlamaydi)
        String authHeader = requestTokenProvider.resolveAuthorizationHeader();
        return ApiResponse.ok(homeService.getHome(authHeader));
    }
}
