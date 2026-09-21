package mexa.club.productservice.repository;

import mexa.club.productservice.entity.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BrandRepository extends JpaRepository<Brand, UUID> {

    boolean existsByNameIgnoreCase(String name);

    Page<Brand> findAllByOrderByNameAsc(Pageable pageable);

    Page<Brand> findByActiveOrderByNameAsc(boolean active, Pageable pageable);

    Page<Brand> findByNameContainingIgnoreCaseOrderByNameAsc(String name, Pageable pageable);

    Page<Brand> findByNameContainingIgnoreCaseAndActiveOrderByNameAsc(String name, boolean active, Pageable pageable);
}
