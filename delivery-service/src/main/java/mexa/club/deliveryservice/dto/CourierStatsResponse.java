package mexa.club.deliveryservice.dto;

public record CourierStatsResponse(
        long totalCouriers,
        long activeCouriers,
        long totalCurrentDeliveries,
        long todayDeliveries,
        long weeklyDeliveries,
        long monthlyDeliveries,
        double successRate
) {}
