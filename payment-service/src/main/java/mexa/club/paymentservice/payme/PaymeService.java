package mexa.club.paymentservice.payme;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import mexa.club.paymentservice.client.OrderServiceClient;
import mexa.club.paymentservice.order.Order;
import mexa.club.paymentservice.order.OrderRepository;
import mexa.club.paymentservice.order.OrderState;
import mexa.club.paymentservice.payme.dto.JsonRpcRequest;
import mexa.club.paymentservice.payme.dto.JsonRpcResponse;
import mexa.club.paymentservice.transaction.PaymeTransaction;
import mexa.club.paymentservice.transaction.PaymeTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymeService {

    private final OrderRepository orderRepository;
    private final PaymeTransactionRepository txRepository;
    private final OrderServiceClient orderServiceClient;

    @Transactional(readOnly = true)
    public JsonRpcResponse checkPerform(JsonRpcRequest req) {
        Object rid = req.getId();
        Map<String, Object> params = req.paramsOrEmpty();

        Optional<JsonRpcResponse> badAmount = requireAmount(params.get("amount"), rid);
        if (badAmount.isPresent()) {
            return badAmount.get();
        }
        long amount = toLong(params.get("amount"));

        Optional<JsonRpcResponse> badAcc = requireAccount(params.get("account"), rid);
        if (badAcc.isPresent()) {
            return badAcc.get();
        }

        Optional<Long> orderIdOpt = parseOrderId(((Map<?, ?>) params.get("account")).get("order_id"));
        if (orderIdOpt.isEmpty()) {
            return JsonRpcResponse.fail(rid, PaymeErrors.invalidParams("order_id"));
        }

        Optional<Order> orderOpt = orderRepository.findById(orderIdOpt.get());
        if (orderOpt.isEmpty()) {
            return JsonRpcResponse.fail(rid, PaymeErrors.order());
        }
        Order order = orderOpt.get();

        if (!order.getAmount().equals(amount)) {
            return JsonRpcResponse.fail(rid, PaymeErrors.amount());
        }

        if (order.getState() == OrderState.PAID || order.getState() == OrderState.CANCELLED) {
            return JsonRpcResponse.fail(rid, PaymeErrors.cantDo());
        }

        return JsonRpcResponse.ok(rid, Map.of("allow", true));
    }

    @Transactional
    public JsonRpcResponse createTransaction(JsonRpcRequest req) {
        Object rid = req.getId();
        Map<String, Object> params = req.paramsOrEmpty();

        Optional<JsonRpcResponse> badId = requirePaycomTxId(params.get("id"), rid);
        if (badId.isPresent()) {
            return badId.get();
        }
        String paycomTxId = String.valueOf(params.get("id"));

        Optional<JsonRpcResponse> badAmount = requireAmount(params.get("amount"), rid);
        if (badAmount.isPresent()) {
            return badAmount.get();
        }
        long amount = toLong(params.get("amount"));

        Optional<JsonRpcResponse> badTime = requireTime(params.get("time"), rid);
        if (badTime.isPresent()) {
            return badTime.get();
        }
        long createTimeMs = toLong(params.get("time"));

        Optional<JsonRpcResponse> badAcc = requireAccount(params.get("account"), rid);
        if (badAcc.isPresent()) {
            return badAcc.get();
        }

        Optional<Long> orderIdOpt = parseOrderId(((Map<?, ?>) params.get("account")).get("order_id"));
        if (orderIdOpt.isEmpty()) {
            return JsonRpcResponse.fail(rid, PaymeErrors.invalidParams("order_id"));
        }
        Long orderId = orderIdOpt.get();

        Order order =
                orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null) {
            return JsonRpcResponse.fail(rid, PaymeErrors.order());
        }

        Optional<PaymeTransaction> existing = txRepository.findByPaycomTxId(paycomTxId);
        if (existing.isPresent()) {
            PaymeTransaction t = existing.get();
            if (!t.getOrderId().equals(orderId) || !t.getAmount().equals(amount)) {
                return JsonRpcResponse.fail(rid, PaymeErrors.cantDo());
            }
            return JsonRpcResponse.ok(rid, createTxResult(t));
        }

        if (txRepository.existsBlockingTransaction(orderId, paycomTxId, List.of((short) 1, (short) 2))) {
            return JsonRpcResponse.fail(rid, PaymeErrors.cantDo());
        }

        if (!order.getAmount().equals(amount)) {
            return JsonRpcResponse.fail(rid, PaymeErrors.amount());
        }

        if (order.getState() == OrderState.PAID || order.getState() == OrderState.CANCELLED) {
            return JsonRpcResponse.fail(rid, PaymeErrors.cantDo());
        }

        PaymeTransaction tx = new PaymeTransaction();
        tx.setPaycomTxId(paycomTxId);
        tx.setOrderId(orderId);
        tx.setAmount(amount);
        tx.setState((short) 1);
        tx.setCreateTime(createTimeMs);
        txRepository.save(tx);

        order.setState(OrderState.WAITING);
        orderRepository.save(order);

        return JsonRpcResponse.ok(rid, createTxResult(tx));
    }

    @Transactional
    public JsonRpcResponse performTransaction(JsonRpcRequest req) {
        Object rid = req.getId();
        Map<String, Object> params = req.paramsOrEmpty();

        Optional<JsonRpcResponse> badId = requirePaycomTxId(params.get("id"), rid);
        if (badId.isPresent()) {
            return badId.get();
        }
        String paycomTxId = String.valueOf(params.get("id"));

        PaymeTransaction tx =
                txRepository.findByPaycomTxIdForUpdate(paycomTxId).orElse(null);
        if (tx == null) {
            return JsonRpcResponse.fail(rid, PaymeErrors.txNotFound());
        }

        if (tx.getState() == 2) {
            return JsonRpcResponse.ok(
                    rid,
                    Map.of(
                            "transaction", String.valueOf(tx.getId()),
                            "perform_time", tx.getPerformTime(),
                            "state", 2));
        }

        if (tx.getState() != 1) {
            return JsonRpcResponse.fail(rid, PaymeErrors.cantDo());
        }

        long now = System.currentTimeMillis();
        tx.setState((short) 2);
        tx.setPerformTime(now);
        txRepository.save(tx);

        Order order = orderRepository.findByIdForUpdate(tx.getOrderId()).orElseThrow();
        order.setState(OrderState.PAID);
        orderRepository.save(order);

        if (order.getOrderServiceId() != null) {
            orderServiceClient.confirmPayment(order.getOrderServiceId());
        }

        return JsonRpcResponse.ok(
                rid,
                Map.of(
                        "transaction", String.valueOf(tx.getId()),
                        "perform_time", now,
                        "state", 2));
    }

    @Transactional
    public JsonRpcResponse cancelTransaction(JsonRpcRequest req) {
        Object rid = req.getId();
        Map<String, Object> params = req.paramsOrEmpty();

        Optional<JsonRpcResponse> badId = requirePaycomTxId(params.get("id"), rid);
        if (badId.isPresent()) {
            return badId.get();
        }
        String paycomTxId = String.valueOf(params.get("id"));

        Object reasonRaw = params.get("reason");
        if (!(reasonRaw instanceof Number)) {
            return JsonRpcResponse.fail(rid, PaymeErrors.invalidParams("reason"));
        }
        int reason = ((Number) reasonRaw).intValue();

        PaymeTransaction tx =
                txRepository.findByPaycomTxIdForUpdate(paycomTxId).orElse(null);
        if (tx == null) {
            return JsonRpcResponse.fail(rid, PaymeErrors.txNotFound());
        }

        if (tx.getState() != null && tx.getState() < 0) {
            return JsonRpcResponse.ok(
                    rid,
                    Map.of(
                            "transaction", String.valueOf(tx.getId()),
                            "cancel_time", Optional.ofNullable(tx.getCancelTime()).orElse(0L),
                            "state", tx.getState()));
        }

        if (tx.getState() != 1 && tx.getState() != 2) {
            return JsonRpcResponse.fail(rid, PaymeErrors.cantCancel());
        }

        int prev = tx.getState();
        int newState = prev == 1 ? -1 : -2;
        long now = System.currentTimeMillis();

        tx.setState((short) newState);
        tx.setReason(reason);
        tx.setCancelTime(now);
        txRepository.save(tx);

        Order order = orderRepository.findByIdForUpdate(tx.getOrderId()).orElseThrow();
        if (newState == -2) {
            order.setState(OrderState.CANCELLED);
        } else if (order.getState() == OrderState.WAITING) {
            order.setState(OrderState.NEW);
        }
        orderRepository.save(order);

        if (order.getOrderServiceId() != null) {
            orderServiceClient.failPayment(order.getOrderServiceId());
        }

        return JsonRpcResponse.ok(
                rid,
                Map.of(
                        "transaction", String.valueOf(tx.getId()),
                        "cancel_time", now,
                        "state", newState));
    }

    @Transactional(readOnly = true)
    public JsonRpcResponse checkTransaction(JsonRpcRequest req) {
        Object rid = req.getId();
        Map<String, Object> params = req.paramsOrEmpty();

        Optional<JsonRpcResponse> badId = requirePaycomTxId(params.get("id"), rid);
        if (badId.isPresent()) {
            return badId.get();
        }
        String paycomTxId = String.valueOf(params.get("id"));

        PaymeTransaction tx = txRepository.findByPaycomTxId(paycomTxId).orElse(null);
        if (tx == null) {
            return JsonRpcResponse.fail(rid, PaymeErrors.txNotFound());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("create_time", tx.getCreateTime());
        result.put("perform_time", tx.getPerformTime() == null ? 0L : tx.getPerformTime());
        result.put("cancel_time", tx.getCancelTime() == null ? 0L : tx.getCancelTime());
        result.put("transaction", String.valueOf(tx.getId()));
        result.put("state", tx.getState());
        result.put("reason", tx.getReason());
        return JsonRpcResponse.ok(rid, result);
    }

    @Transactional(readOnly = true)
    public JsonRpcResponse getStatement(JsonRpcRequest req) {
        Object rid = req.getId();
        Map<String, Object> params = req.paramsOrEmpty();

        if (params.get("from") == null) {
            return JsonRpcResponse.fail(rid, PaymeErrors.invalidParams("from"));
        }
        if (params.get("to") == null) {
            return JsonRpcResponse.fail(rid, PaymeErrors.invalidParams("to"));
        }
        long from = toLong(params.get("from"));
        long to = toLong(params.get("to"));

        List<PaymeTransaction> list = txRepository.findByCreateTimeBetween(from, to);
        List<Map<String, Object>> out =
                list.stream().map(this::statementRow).toList();

        return JsonRpcResponse.ok(rid, Map.of("transactions", out));
    }

    private Map<String, Object> statementRow(PaymeTransaction t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getPaycomTxId());
        m.put("time", t.getCreateTime());
        m.put("amount", t.getAmount());
        m.put("account", Map.of("order_id", String.valueOf(t.getOrderId())));
        m.put("create_time", t.getCreateTime());
        m.put("perform_time", t.getPerformTime() == null ? 0L : t.getPerformTime());
        m.put("cancel_time", t.getCancelTime() == null ? 0L : t.getCancelTime());
        m.put("transaction", String.valueOf(t.getId()));
        m.put("state", t.getState());
        m.put("reason", t.getReason());
        return m;
    }

    private static Map<String, Object> createTxResult(PaymeTransaction t) {
        return Map.of(
                "create_time",
                t.getCreateTime(),
                "transaction",
                String.valueOf(t.getId()),
                "state",
                t.getState());
    }

    private static Optional<JsonRpcResponse> requirePaycomTxId(Object raw, Object rid) {
        if (raw == null || String.valueOf(raw).isBlank()) {
            return Optional.of(JsonRpcResponse.fail(rid, PaymeErrors.invalidParams("id")));
        }
        return Optional.empty();
    }

    private static Optional<JsonRpcResponse> requireAmount(Object raw, Object rid) {
        if (raw == null) {
            return Optional.of(JsonRpcResponse.fail(rid, PaymeErrors.invalidParams("amount")));
        }
        try {
            long v = toLong(raw);
            if (v <= 0) {
                return Optional.of(JsonRpcResponse.fail(rid, PaymeErrors.amount()));
            }
        } catch (IllegalArgumentException ex) {
            return Optional.of(JsonRpcResponse.fail(rid, PaymeErrors.invalidParams("amount")));
        }
        return Optional.empty();
    }

    private static Optional<JsonRpcResponse> requireTime(Object raw, Object rid) {
        if (raw == null) {
            return Optional.of(JsonRpcResponse.fail(rid, PaymeErrors.invalidParams("time")));
        }
        try {
            toLong(raw);
        } catch (IllegalArgumentException ex) {
            return Optional.of(JsonRpcResponse.fail(rid, PaymeErrors.invalidParams("time")));
        }
        return Optional.empty();
    }

    private static Optional<JsonRpcResponse> requireAccount(Object raw, Object rid) {
        if (!(raw instanceof Map<?, ?>)) {
            return Optional.of(JsonRpcResponse.fail(rid, PaymeErrors.invalidParams("account")));
        }
        return Optional.empty();
    }

    private static Optional<Long> parseOrderId(Object raw) {
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(toLong(raw));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static long toLong(Object raw) {
        if (raw instanceof Number n) {
            return n.longValue();
        }
        String s = String.valueOf(raw).trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException();
        }
        return Long.parseLong(s);
    }
}
