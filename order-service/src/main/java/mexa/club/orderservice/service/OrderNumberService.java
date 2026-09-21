package mexa.club.orderservice.service;

import mexa.club.orderservice.entity.OrderSequence;
import mexa.club.orderservice.repository.OrderSequenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Service
public class OrderNumberService {
    private final OrderSequenceRepository orderSequenceRepository;

    public OrderNumberService(OrderSequenceRepository orderSequenceRepository) {
        this.orderSequenceRepository = orderSequenceRepository;
    }

    @Transactional
    public String nextOrderNumber() {
        int year = Year.now().getValue();
        OrderSequence sequence = orderSequenceRepository.findByYearForUpdate(year).orElseGet(() -> {
            OrderSequence fresh = new OrderSequence();
            fresh.setYear(year);
            fresh.setValue(0L);
            return fresh;
        });
        sequence.setValue(sequence.getValue() + 1);
        orderSequenceRepository.save(sequence);
        return "ORD-%d-%05d".formatted(year, sequence.getValue());
    }
}
