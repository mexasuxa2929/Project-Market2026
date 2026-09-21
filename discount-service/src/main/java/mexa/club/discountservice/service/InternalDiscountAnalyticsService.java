package mexa.club.discountservice.service;

import mexa.club.discountservice.dto.InternalDiscountStatsResponse;
import mexa.club.discountservice.repository.PromotionRepository;
import mexa.club.discountservice.repository.PromotionUsageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class InternalDiscountAnalyticsService {

    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository promotionUsageRepository;

    public InternalDiscountAnalyticsService(
            PromotionRepository promotionRepository,
            PromotionUsageRepository promotionUsageRepository
    ) {
        this.promotionRepository = promotionRepository;
        this.promotionUsageRepository = promotionUsageRepository;
    }

    @Transactional(readOnly = true)
    public InternalDiscountStatsResponse stats(LocalDateTime from, LocalDateTime to, LocalDateTime now) {
        long active = promotionRepository.countCurrentlyActive(now);
        BigDecimal totalGiven = promotionUsageRepository.sumDiscountBetween(from, to);
        List<Object[]> rows = promotionUsageRepository.topPromotionsBetween(from, to, PageRequest.of(0, 5));
        List<InternalDiscountStatsResponse.TopPromotionStat> top = rows.stream()
                .map(r -> new InternalDiscountStatsResponse.TopPromotionStat(
                        String.valueOf(r[0]),
                        ((Number) r[1]).longValue(),
                        (BigDecimal) r[2]
                ))
                .toList();
        return new InternalDiscountStatsResponse(active, totalGiven, top);
    }
}
