package mexa.club.paymentservice.order.dto;

public record OrderCreatedResponse(Long orderId, Long amountTiyin, String checkoutUrl) {}
