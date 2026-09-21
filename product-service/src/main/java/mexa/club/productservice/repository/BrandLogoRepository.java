package mexa.club.productservice.repository;

import mexa.club.productservice.entity.BrandLogo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BrandLogoRepository extends JpaRepository<BrandLogo, UUID> {
}
