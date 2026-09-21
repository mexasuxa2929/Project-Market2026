package mexa.club.shopservice.dto;

import java.util.List;

public record CartResponse(List<CartLineResponse> lines, int totalQuantity, long lineCount) {
}
