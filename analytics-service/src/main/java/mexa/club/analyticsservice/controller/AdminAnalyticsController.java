package mexa.club.analyticsservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import mexa.club.analyticsservice.dto.DashboardSummaryResponse;
import mexa.club.analyticsservice.dto.DiscountSummaryResponse;
import mexa.club.analyticsservice.dto.OrderStatusBreakdownResponse;
import mexa.club.analyticsservice.dto.RevenueChartResponse;
import mexa.club.analyticsservice.dto.StockAlertResponse;
import mexa.club.analyticsservice.dto.TopProductsResponse;
import mexa.club.analyticsservice.dto.UserGrowthResponse;
import mexa.club.analyticsservice.service.AnalyticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/analytics")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
@Tag(name = "Admin - Analytics", description = "Dashboard and reporting endpoints for administrators. Aggregates data from orders, products, users, and discounts. Requires the ANALYTICS_VIEW authority.")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    public AdminAnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    @Operation(
            summary = "Get dashboard summary",
            description = "Returns a high-level summary of key business metrics including total revenue, order counts, active users, and top products. Results are cached for performance; pass refresh=true to force recomputation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard summary returned successfully"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ANALYTICS_VIEW authority")
    })
    public DashboardSummaryResponse summary(
            @Parameter(description = "Set to true to bypass the cache and recompute fresh metrics", example = "false") @RequestParam(defaultValue = "false") boolean refresh
    ) {
        return analyticsService.getCachedSummaryOrCompute(refresh);
    }

    @GetMapping("/revenue/chart")
    @Operation(
            summary = "Get revenue chart data",
            description = "Returns time-series revenue data suitable for rendering a chart. The granularity and range depend on the period parameter. Supported values: today/daily, week/weekly/7d/last_7_days, month/monthly/30d/last_30_days, quarterly/90d/last_90_days, year/yearly/this_year, or an ISO date (2026-05-20)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Revenue chart data returned successfully"),
            @ApiResponse(responseCode = "400", description = "Unrecognised period value"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ANALYTICS_VIEW authority")
    })
    public RevenueChartResponse revenueChart(
            @Parameter(description = "Reporting period (e.g., today, week, month, year, or ISO date)", required = true, example = "month") @RequestParam String period
    ) {
        return analyticsService.revenueChart(period);
    }

    @GetMapping("/orders/breakdown")
    @Operation(
            summary = "Get order status breakdown",
            description = "Returns the count of orders grouped by status (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED, etc.) for the given period."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order status breakdown returned successfully"),
            @ApiResponse(responseCode = "400", description = "Unrecognised period value"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ANALYTICS_VIEW authority")
    })
    public OrderStatusBreakdownResponse ordersBreakdown(
            @Parameter(description = "Reporting period (e.g., today, week, month, year, or ISO date)", required = true, example = "week") @RequestParam String period
    ) {
        return analyticsService.orderBreakdown(period);
    }

    @GetMapping("/products/top")
    @Operation(
            summary = "Get top-selling products",
            description = "Returns the products with the highest sales volume (units sold) for the given period. The number of results is controlled by the limit parameter."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Top products list returned successfully"),
            @ApiResponse(responseCode = "400", description = "Unrecognised period value"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ANALYTICS_VIEW authority")
    })
    public TopProductsResponse productsTop(
            @Parameter(description = "Reporting period (e.g., today, week, month, year, or ISO date)", required = true, example = "month") @RequestParam String period,
            @Parameter(description = "Maximum number of products to return", example = "10") @RequestParam(defaultValue = "10") int limit
    ) {
        return analyticsService.productsTop(period, limit);
    }

    @GetMapping("/stock/alerts")
    @Operation(
            summary = "Get low-stock alerts",
            description = "Returns all products whose current stock level is at or below the specified threshold. Useful for inventory management and re-order planning."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Low-stock alerts returned successfully"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ANALYTICS_VIEW authority")
    })
    public StockAlertResponse stockAlerts(
            @Parameter(description = "Stock quantity threshold; products at or below this level are included in the result", example = "10") @RequestParam(defaultValue = "10") int threshold
    ) {
        return analyticsService.stockAlerts(threshold);
    }

    @GetMapping("/users/growth")
    @Operation(
            summary = "Get user growth data",
            description = "Returns time-series data showing the number of new user registrations over the specified period, suitable for plotting a user growth chart."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User growth data returned successfully"),
            @ApiResponse(responseCode = "400", description = "Unrecognised period value"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ANALYTICS_VIEW authority")
    })
    public UserGrowthResponse userGrowth(
            @Parameter(description = "Reporting period (e.g., today, week, month, year, or ISO date)", required = true, example = "month") @RequestParam String period
    ) {
        return analyticsService.userGrowth(period);
    }

    @GetMapping("/discounts/summary")
    @Operation(
            summary = "Get discount and promotion summary",
            description = "Returns a summary of discount and promotion activity including total discount amounts granted, number of promotions used, and the most popular promotion codes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Discount summary returned successfully"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ANALYTICS_VIEW authority")
    })
    public DiscountSummaryResponse discountsSummary() {
        return analyticsService.discountSummary();
    }
}
