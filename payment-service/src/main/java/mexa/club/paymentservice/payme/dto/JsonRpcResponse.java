package mexa.club.paymentservice.payme.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRpcResponse {

    private Object id;
    private Object result;
    private JsonRpcError error;

    public static JsonRpcResponse ok(Object id, Object result) {
        return new JsonRpcResponse(id, result, null);
    }

    public static JsonRpcResponse fail(Object id, JsonRpcError err) {
        return new JsonRpcResponse(id, null, err);
    }
}
