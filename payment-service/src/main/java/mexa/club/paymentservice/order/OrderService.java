package mexa.club.paymentservice.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public Order create(Long userId, Long amountTiyin, UUID orderServiceId) {
        Order order = new Order(userId, amountTiyin, OrderState.NEW);
        order.setOrderServiceId(orderServiceId);
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public Order requireById(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }
}
