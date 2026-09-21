package mexa.club.deliveryservice.service;

import mexa.club.deliveryservice.dto.DeliveryResponse;
import mexa.club.deliveryservice.dto.TrackingEventResponse;
import mexa.club.deliveryservice.entity.Delivery;
import mexa.club.deliveryservice.entity.DeliveryTrackingEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeliveryMapper {
    public DeliveryResponse toResponse(Delivery d, List<DeliveryTrackingEvent> events) {
        return new DeliveryResponse(
                d.getId(), d.getOrderId(), d.getCourierId(), d.getStatus(), d.getFromWarehouseId(),
                d.getDeliveryAddress(), d.getRecipientName(), d.getRecipientPhone(), d.getRegion(), d.getDistrict(),
                d.getEstimatedDelivery(), d.getActualDelivery(), d.getDeliveryFee(), d.getTrackingCode(), d.getNote(),
                d.getFailReason(), d.getCreatedAt(), d.getUpdatedAt(),
                events.stream().map(this::toEvent).toList()
        );
    }

    private TrackingEventResponse toEvent(DeliveryTrackingEvent e) {
        return new TrackingEventResponse(e.getId(), e.getStatus(), e.getLocation(), e.getDescription(), e.getCreatedAt());
    }
}
