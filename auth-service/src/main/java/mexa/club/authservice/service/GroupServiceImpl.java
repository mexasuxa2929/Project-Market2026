package mexa.club.authservice.service;

import lombok.RequiredArgsConstructor;
import mexa.club.authservice.dto.AddMemberRequest;
import mexa.club.authservice.dto.GroupDetailResponse;
import mexa.club.authservice.dto.GroupMemberResponse;
import mexa.club.authservice.dto.GroupRequest;
import mexa.club.authservice.dto.GroupResponse;
import mexa.club.authservice.entity.GroupMember;
import mexa.club.authservice.entity.GroupMemberRole;
import mexa.club.authservice.entity.User;
import mexa.club.authservice.entity.UserGroup;
import mexa.club.authservice.repository.GroupMemberRepository;
import mexa.club.authservice.repository.GroupRepository;
import mexa.club.authservice.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    @Override
    public GroupResponse createGroup(GroupRequest request) {
        if (groupRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Group already exists: " + request.name());
        }

        UserGroup group = new UserGroup();
        group.setName(request.name());
        group.setDescription(request.description());
        group.setCreatedBy(currentUsername());

        return groupToResponse(groupRepository.save(group));
    }

    @Override
    public GroupResponse updateGroup(UUID id, GroupRequest request) {
        UserGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Group not found: " + id));

        if (request.name() != null && !request.name().isBlank() && !request.name().equals(group.getName())) {
            if (groupRepository.existsByName(request.name())) {
                throw new IllegalArgumentException("Group already exists: " + request.name());
            }
            group.setName(request.name());
        }
        if (request.description() != null) {
            group.setDescription(request.description());
        }

        return groupToResponse(groupRepository.save(group));
    }

    @Override
    public void deleteGroup(UUID id) {
        UserGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Group not found: " + id));
        groupRepository.delete(group);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupDetailResponse getGroup(UUID id) {
        UserGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Group not found: " + id));

        List<GroupMember> members = groupMemberRepository.findByUserGroupId(id);
        List<GroupMemberResponse> memberResponses = members.stream()
                .map(this::memberToResponse)
                .toList();

        return new GroupDetailResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                members.size(),
                group.getCreatedBy(),
                group.getCreatedAt(),
                memberResponses
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupResponse> getAllGroups() {
        return groupRepository.findAll().stream()
                .map(g -> {
                    long count = groupMemberRepository.countByUserGroupId(g.getId());
                    return new GroupResponse(
                            g.getId(),
                            g.getName(),
                            g.getDescription(),
                            (int) count,
                            g.getCreatedBy(),
                            g.getCreatedAt()
                    );
                })
                .toList();
    }

    @Override
    public GroupMemberResponse addMember(UUID groupId, AddMemberRequest request) {
        UserGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Group not found: " + groupId));

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new NoSuchElementException("User not found: " + request.userId()));

        if (groupMemberRepository.existsByUserGroupIdAndUserId(groupId, request.userId())) {
            throw new IllegalArgumentException("User is already a member of this group");
        }

        GroupMemberRole role = GroupMemberRole.MEMBER;
        if (request.roleInGroup() != null) {
            try {
                role = GroupMemberRole.valueOf(request.roleInGroup().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        GroupMember member = new GroupMember();
        member.setUserGroup(group);
        member.setUser(user);
        member.setRoleInGroup(role);

        return memberToResponse(groupMemberRepository.save(member));
    }

    @Override
    public void removeMember(UUID groupId, UUID memberId) {
        GroupMember member = groupMemberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("Member not found: " + memberId));

        if (!member.getUserGroup().getId().equals(groupId)) {
            throw new IllegalArgumentException("Member does not belong to this group");
        }

        groupMemberRepository.delete(member);
    }

    private GroupResponse groupToResponse(UserGroup group) {
        long count = groupMemberRepository.countByUserGroupId(group.getId());
        return new GroupResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                (int) count,
                group.getCreatedBy(),
                group.getCreatedAt()
        );
    }

    private GroupMemberResponse memberToResponse(GroupMember member) {
        return new GroupMemberResponse(
                member.getId(),
                member.getUser().getId(),
                member.getUser().getUsername(),
                member.getUser().getEmail(),
                member.getRoleInGroup().name(),
                member.getJoinedAt()
        );
    }

    private static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : null;
    }
}