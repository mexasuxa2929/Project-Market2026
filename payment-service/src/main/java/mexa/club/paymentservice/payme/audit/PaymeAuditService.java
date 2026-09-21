package mexa.club.paymentservice.payme.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mexa.club.paymentservice.payme.dto.JsonRpcRequest;
import mexa.club.paymentservice.payme.dto.JsonRpcResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymeAuditService {

    private static final int MAX_LEN = 16_384;

    private final PaymeRequestLogRepository logRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(JsonRpcRequest req, JsonRpcResponse resp) {
        try {
            PaymeRequestLog row = new PaymeRequestLog();
            row.setRequestId(req.getId() == null ? null : String.valueOf(req.getId()));
            row.setMethod(req.getMethod());
            row.setRequestPayload(truncate(json(req)));
            row.setResponsePayload(truncate(json(resp)));
            logRepository.save(row);
        } catch (Exception e) {
            log.warn("payme audit log failed: {}", e.getMessage());
        }
        log.info(
                "payme rpc request_id={} method={}",
                req.getId(),
                req.getMethod());
    }

    private String json(Object o) throws JsonProcessingException {
        return objectMapper.writeValueAsString(o);
    }

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= MAX_LEN ? s : s.substring(0, MAX_LEN);
    }
}
