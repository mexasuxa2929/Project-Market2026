package mexa.club.productservice.repository;

import mexa.club.productservice.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    boolean existsByNameIgnoreCase(String name);

    Page<Category> findAllByOrderByNameAsc(Pageable pageable);

    Page<Category> findByActiveOrderByNameAsc(boolean active, Pageable pageable);

    Page<Category> findByNameContainingIgnoreCaseOrderByNameAsc(String name, Pageable pageable);

    Page<Category> findByNameContainingIgnoreCaseAndActiveOrderByNameAsc(String name, boolean active, Pageable pageable);

    @Query("SELECT c FROM Category c WHERE c.parentId IS NULL AND c.active = true ORDER BY c.name ASC")
    List<Category> findRootCategories();

    @Query("SELECT c FROM Category c WHERE c.parentId = :parentId ORDER BY c.name ASC")
    List<Category> findByParent(@Param("parentId") UUID parentId);
}
