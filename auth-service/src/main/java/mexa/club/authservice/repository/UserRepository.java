package mexa.club.authservice.repository;

import mexa.club.authservice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    @EntityGraph(attributePaths = "roles")
    @Override
    Page<User> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "roles")
    @Query("""
            select u
            from User u
            where not exists (
                select 1
                from User ux join ux.roles r
                where ux.id = u.id and r.name = :roleName
            )
            """)
    Page<User> findAllExcludingRole(@Param("roleName") String roleName, Pageable pageable);

    @EntityGraph(attributePaths = "roles")
    @Override
    Optional<User> findById(UUID id);

    @EntityGraph(attributePaths = "roles")
    @Query("""
            select u
            from User u
            where u.id = :id
              and not exists (
                select 1
                from User ux join ux.roles r
                where ux.id = u.id and r.name = :roleName
            )
            """)
    Optional<User> findByIdExcludingRole(@Param("id") UUID id, @Param("roleName") String roleName);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByUsername(String username);

    @Query("select distinct u from User u left join fetch u.roles r left join fetch r.permissions where u.username = :username")
    Optional<User> findByUsernameFetchingRoles(@Param("username") String username);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "roles")
    @Query("""
            select u
            from User u
            where lower(u.email) = lower(:email)
              and not exists (
                select 1
                from User ux join ux.roles r
                where ux.id = u.id and r.name = :roleName
            )
            """)
    Optional<User> findByEmailIgnoreCaseExcludingRole(@Param("email") String email, @Param("roleName") String roleName);

    Optional<User> findByGoogleId(String googleId);

    @EntityGraph(attributePaths = "roles")
    @Query("select u from User u join u.roles r where r.name = :roleName")
    List<User> findByRoles_Name(@Param("roleName") String roleName);

    boolean existsByUsername(String username);

    List<User> findByVerifiedFalseAndCreatedAtBefore(LocalDateTime threshold);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    long countByVerifiedTrue();

    @Query(value = """
            WITH days AS (
                SELECT (gs.d)::date AS day
                FROM generate_series(:from::timestamp, :to::timestamp, interval '1 day') AS gs(d)
            )
            SELECT d.day,
                   COALESCE(n.cnt, 0),
                   (
                       SELECT COUNT(*)
                       FROM users u
                       WHERE u.created_at IS NULL OR CAST(u.created_at AS date) <= d.day
                   )
            FROM days d
            LEFT JOIN (
                SELECT CAST(u.created_at AS date) AS day, COUNT(*) AS cnt
                FROM users u
                WHERE u.created_at IS NOT NULL
                GROUP BY CAST(u.created_at AS date)
            ) n ON n.day = d.day
            ORDER BY d.day
            """, nativeQuery = true)
    List<Object[]> userGrowthSeries(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
