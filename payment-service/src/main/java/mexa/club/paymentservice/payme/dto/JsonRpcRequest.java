package mexa.club.paymentservice.payme.dto;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class JsonRpcRequest {

    private Object id;
    private String method;
    private Map<String, Object> params;

    public Map<String, Object> paramsOrEmpty() {
        return params == null ? Map.of() : params;
    }

    /** Mutatsiyasiz ko'rinish uchun (audit). */
    public Map<String, Object> safeParamsCopy() {
        return params == null ? Map.of() : new HashMap<>(params);
    }
}
