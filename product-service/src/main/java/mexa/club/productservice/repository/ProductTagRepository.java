package mexa.club.productservice.repository;

import mexa.club.productservice.entity.ProductTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductTagRepository extends JpaRepository<ProductTag, UUID> {
    Optional<ProductTag> findByName(String name);
    List<ProductTag> findAllByOrderByNameAsc();
}
