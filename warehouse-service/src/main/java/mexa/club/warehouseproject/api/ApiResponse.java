package mexa.club.warehouseproject.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standart API javobi: {@code success}, {@code data}, {@code error}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorPayload error;

    private ApiResponse(boolean success, T data, ErrorPayload error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorPayload(code, message, null));
    }

    public static <T> ApiResponse<T> fail(String code, String message, java.util.Map<String, String> fieldErrors) {
        return new ApiResponse<>(false, null, new ErrorPayload(code, message, fieldErrors));
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public ErrorPayload getError() {
        return error;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorPayload(String code, String message, java.util.Map<String, String> fieldErrors) {}
}
