package mexa.club.orderservice.dto;

import jakarta.validation.constraints.Size;

public record AdminNoteRequest(
        @Size(max = 1000) String note
) {}
