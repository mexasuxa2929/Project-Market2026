package mexa.club.shopservice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record WishlistAddRequest(@NotNull UUID productId) {
}
