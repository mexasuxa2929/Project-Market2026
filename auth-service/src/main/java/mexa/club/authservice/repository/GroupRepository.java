package mexa.club.authservice.repository;

import mexa.club.authservice.entity.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<UserGroup, UUID> {
    boolean existsByName(String name);
    Optional<UserGroup> findByName(String name);
}