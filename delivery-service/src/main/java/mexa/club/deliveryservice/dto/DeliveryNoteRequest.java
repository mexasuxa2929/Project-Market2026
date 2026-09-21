package mexa.club.deliveryservice.dto;

import jakarta.validation.constraints.Size;

public record DeliveryNoteRequest(
        @Size(max = 500) String note
) {}
