package mexa.club.paymentservice.order;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mexa.club.paymentservice.order.dto.CreateOrderRequest;
import mexa.club.paymentservice.order.dto.OrderCreatedResponse;
import mexa.club.paymentservice.payme.CheckoutLinkBuilder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payment - Orders", description = "Order creation and Payme checkout link generation for the payment service")
@RestController
@RequestMapping(value = "/api/orders", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CheckoutLinkBuilder checkoutLinkBuilder;

    /** Minimal buyurtma yaratish + Payme checkout havolasi (sandbox sinovlari uchun). */
    @Operation(
        summary = "Create an order and get Payme checkout URL",
        description = "Creates a minimal payment order record and returns a Payme checkout URL. The client should redirect the user to this URL to complete payment. Primarily used for sandbox testing."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order created and checkout URL generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid order request")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public OrderCreatedResponse create(@Valid @RequestBody CreateOrderRequest body) {
        Order order = orderService.create(body.userId(), body.amountTiyin(), body.orderServiceId());
        String url = checkoutLinkBuilder.buildCheckoutUrl(order.getId(), order.getAmount(), body.returnUrl());
        return new OrderCreatedResponse(order.getId(), order.getAmount(), url);
    }
}
