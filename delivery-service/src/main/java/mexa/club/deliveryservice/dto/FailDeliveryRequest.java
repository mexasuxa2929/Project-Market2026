package mexa.club.deliveryservice.dto;

import jakarta.validation.constraints.NotBlank;

public record FailDeliveryRequest(@NotBlank String reason) {}
