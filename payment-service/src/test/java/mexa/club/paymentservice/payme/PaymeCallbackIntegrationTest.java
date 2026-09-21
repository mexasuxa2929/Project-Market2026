package mexa.club.paymentservice.payme;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import mexa.club.paymentservice.client.OrderServiceClient;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymeCallbackIntegrationTest {

    @MockBean
    private OrderServiceClient orderServiceClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String basicTestAuth() {
        String raw = "Paycom:test_payme_secret";
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private long createOrder(long amount) throws Exception {
        String body =
                objectMapper.writeValueAsString(
                        Map.of(
                                "userId",
                                1,
                                "amountTiyin",
                                amount,
                                "returnUrl",
                                "https://example.com/ok",
                                "orderServiceId",
                                UUID.fromString("11111111-1111-4111-8111-111111111111")));
        String json =
                mockMvc.perform(
                                post("/api/orders")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return objectMapper.readTree(json).get("orderId").asLong();
    }

    @Test
    void authMissing_returns32504() throws Exception {
        mockMvc.perform(
                        post("/payment/payme/callback")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"id\":1,\"method\":\"CheckPerformTransaction\",\"params\":{}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32504));
    }

    @Test
    void checkPerform_orderMissing_returns31050() throws Exception {
        mockMvc.perform(
                        post("/payment/payme/callback")
                                .header("Authorization", basicTestAuth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of(
                                                        "id",
                                                        2,
                                                        "method",
                                                        "CheckPerformTransaction",
                                                        "params",
                                                        Map.of(
                                                                "amount",
                                                                1000,
                                                                "account",
                                                                Map.of("order_id", "999999999"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-31050));
    }

    @Test
    void checkPerform_wrongAmount_returns31001() throws Exception {
        long orderId = createOrder(50_000);
        mockMvc.perform(
                        post("/payment/payme/callback")
                                .header("Authorization", basicTestAuth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of(
                                                        "id",
                                                        3,
                                                        "method",
                                                        "CheckPerformTransaction",
                                                        "params",
                                                        Map.of(
                                                                "amount",
                                                                999,
                                                                "account",
                                                                Map.of("order_id", String.valueOf(orderId)))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-31001));
    }

    @Test
    void checkPerform_ok_allowTrue() throws Exception {
        long orderId = createOrder(77_700);
        mockMvc.perform(
                        post("/payment/payme/callback")
                                .header("Authorization", basicTestAuth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of(
                                                        "id",
                                                        4,
                                                        "method",
                                                        "CheckPerformTransaction",
                                                        "params",
                                                        Map.of(
                                                                "amount",
                                                                77_700,
                                                                "account",
                                                                Map.of("order_id", String.valueOf(orderId)))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.allow").value(true));
    }

    @Test
    void create_perform_cancel_flow_and_idempotency() throws Exception {
        long amount = 120_000;
        long orderId = createOrder(amount);
        String paycomId = "paycom-tx-1";
        long paycomTime = System.currentTimeMillis();

        String createBody =
                objectMapper.writeValueAsString(
                        Map.of(
                                "id",
                                10,
                                "method",
                                "CreateTransaction",
                                "params",
                                Map.of(
                                        "id",
                                        paycomId,
                                        "time",
                                        paycomTime,
                                        "amount",
                                        amount,
                                        "account",
                                        Map.of("order_id", String.valueOf(orderId)))));

        String createResp =
                mockMvc.perform(
                                post("/payment/payme/callback")
                                        .header("Authorization", basicTestAuth())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(createBody))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.result.state").value(1))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        mockMvc.perform(
                        post("/payment/payme/callback")
                                .header("Authorization", basicTestAuth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.state").value(1))
                .andExpect(jsonPath("$.result.transaction").value(objectMapper.readTree(createResp).path("result").path("transaction").asText()));

        mockMvc.perform(
                        post("/payment/payme/callback")
                                .header("Authorization", basicTestAuth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of(
                                                        "id",
                                                        11,
                                                        "method",
                                                        "CreateTransaction",
                                                        "params",
                                                        Map.of(
                                                                "id",
                                                                "paycom-tx-2",
                                                                "time",
                                                                paycomTime,
                                                                "amount",
                                                                amount,
                                                                "account",
                                                                Map.of("order_id", String.valueOf(orderId)))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-31008));

        mockMvc.perform(
                        post("/payment/payme/callback")
                                .header("Authorization", basicTestAuth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of(
                                                        "id",
                                                        12,
                                                        "method",
                                                        "PerformTransaction",
                                                        "params",
                                                        Map.of("id", paycomId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.state").value(2));

        mockMvc.perform(
                        post("/payment/payme/callback")
                                .header("Authorization", basicTestAuth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of(
                                                        "id",
                                                        13,
                                                        "method",
                                                        "PerformTransaction",
                                                        "params",
                                                        Map.of("id", paycomId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.state").value(2));

        mockMvc.perform(
                        post("/payment/payme/callback")
                                .header("Authorization", basicTestAuth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of(
                                                        "id",
                                                        14,
                                                        "method",
                                                        "CheckTransaction",
                                                        "params",
                                                        Map.of("id", paycomId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.transaction").exists())
                .andExpect(jsonPath("$.result.perform_time").isNumber())
                .andExpect(jsonPath("$.result.cancel_time").value(0));

        mockMvc.perform(
                        post("/payment/payme/callback")
                                .header("Authorization", basicTestAuth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of(
                                                        "id",
                                                        15,
                                                        "method",
                                                        "CancelTransaction",
                                                        "params",
                                                        Map.of("id", paycomId, "reason", 4)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.state").value(-2));

        long orderId2 = createOrder(50_000);
        String paycomIdB = "paycom-tx-before-perform";
        mockMvc.perform(
                        post("/payment/payme/callback")
                                .header("Authorization", basicTestAuth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of(
                                                        "id",
                                                        16,
                                                        "method",
                                                        "CreateTransaction",
                                                        "params",
                                                        Map.of(
                                                                "id",
                                                                paycomIdB,
                                                                "time",
                                                                System.currentTimeMillis(),
                                                                "amount",
                                                                50_000,
                                                                "account",
                                                                Map.of("order_id", String.valueOf(orderId2)))))))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/payment/payme/callback")
                                .header("Authorization", basicTestAuth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of(
                                                        "id",
                                                        17,
                                                        "method",
                                                        "CancelTransaction",
                                                        "params",
                                                        Map.of("id", paycomIdB, "reason", 5)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.state").value(-1));

        long from = paycomTime - 60_000;
        long to = System.currentTimeMillis() + 60_000;
        mockMvc.perform(
                        post("/payment/payme/callback")
                                .header("Authorization", basicTestAuth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of(
                                                        "id",
                                                        18,
                                                        "method",
                                                        "GetStatement",
                                                        "params",
                                                        Map.of("from", from, "to", to)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.transactions").isArray())
                .andExpect(jsonPath("$.result.transactions[0].id").exists());
    }

    @Test
    void unknownMethod_returns32601() throws Exception {
        mockMvc.perform(
                        post("/payment/payme/callback")
                                .header("Authorization", basicTestAuth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of("id", 99, "method", "NoSuchMethod", "params", Map.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32601));
    }
}
