package mexa.club.shopservice.client.payload;

public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorPayload error
) {
    public record ErrorPayload(
            String code,
            String message
    ) {
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /** Success with no body (e.g. DELETE). */
    public static ApiResponse<Void> okVoid() {
        return new ApiResponse<>(true, null, null);
    }
}
