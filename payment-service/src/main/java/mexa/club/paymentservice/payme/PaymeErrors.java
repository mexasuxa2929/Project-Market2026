package mexa.club.paymentservice.payme;

import java.util.Map;
import mexa.club.paymentservice.payme.dto.JsonRpcError;

public final class PaymeErrors {

    private PaymeErrors() {}

    private static Map<String, String> msg(String uz, String ru, String en) {
        return Map.of("uz", uz, "ru", ru, "en", en);
    }

    public static JsonRpcError auth() {
        return new JsonRpcError(
                -32504,
                msg("Yetarli huquq yo'q", "Недостаточно привилегий", "Insufficient privilege"),
                "auth");
    }

    public static JsonRpcError amount() {
        return new JsonRpcError(-31001, msg("Summa noto'g'ri", "Неверная сумма", "Invalid amount"), "amount");
    }

    public static JsonRpcError order() {
        return new JsonRpcError(-31050, msg("Buyurtma topilmadi", "Заказ не найден", "Order not found"), "order_id");
    }

    public static JsonRpcError txNotFound() {
        return new JsonRpcError(
                -31003,
                msg("Tranzaksiya topilmadi", "Транзакция не найдена", "Transaction not found"),
                "transaction");
    }

    public static JsonRpcError cantDo() {
        return new JsonRpcError(-31008, msg("Bajarib bo'lmaydi", "Невозможно выполнить", "Unable to perform"), "transaction");
    }

    public static JsonRpcError cantCancel() {
        return new JsonRpcError(
                -31007,
                msg("Bekor qilib bo'lmaydi", "Невозможно отменить", "Unable to cancel"),
                "transaction");
    }

    public static JsonRpcError parse() {
        return new JsonRpcError(-32700, msg("JSON xato", "Ошибка JSON", "Parse error"), null);
    }

    public static JsonRpcError invalidRequest() {
        return new JsonRpcError(-32600, msg("Noto'g'ri so'rov", "Неверный запрос", "Invalid Request"), null);
    }

    public static JsonRpcError methodNotFound() {
        return new JsonRpcError(-32601, msg("Metod topilmadi", "Метод не найден", "Method not found"), "method");
    }

    public static JsonRpcError invalidParams(String data) {
        return new JsonRpcError(-32602, msg("Param xato", "Неверные параметры", "Invalid params"), data);
    }

    public static JsonRpcError internal() {
        return new JsonRpcError(-32603, msg("Ichki xatolik", "Внутренняя ошибка", "Internal error"), null);
    }
}
