package mexa.club.authservice.repository;

import mexa.club.authservice.entity.GroupMember;
import mexa.club.authservice.entity.GroupMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {
    List<GroupMember> findByUserGroupId(UUID groupId);
    Optional<GroupMember> findByUserGroupIdAndUserId(UUID groupId, UUID userId);
    boolean existsByUserGroupIdAndUserId(UUID groupId, UUID userId);
    long countByUserGroupId(UUID groupId);
    List<GroupMember> findByUserId(UUID userId);
    long countByUserGroupIdAndRoleInGroup(UUID groupId, GroupMemberRole role);
}