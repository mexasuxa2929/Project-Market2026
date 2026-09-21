package mexa.club.discountservice.service;

import mexa.club.discountservice.dto.PromotionResponse;
import mexa.club.discountservice.dto.PromotionUsageResponse;
import mexa.club.discountservice.dto.PublicPromotionResponse;
import mexa.club.discountservice.entity.Promotion;
import mexa.club.discountservice.entity.PromotionUsage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PromotionStructMapper {

    PromotionResponse toResponse(Promotion promotion);

    PublicPromotionResponse toPublic(Promotion promotion);

    @Mapping(target = "promotionId", source = "promotion.id")
    PromotionUsageResponse toUsage(PromotionUsage usage);
}
