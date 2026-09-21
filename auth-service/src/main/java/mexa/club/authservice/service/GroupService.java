package mexa.club.authservice.service;

import mexa.club.authservice.dto.AddMemberRequest;
import mexa.club.authservice.dto.GroupDetailResponse;
import mexa.club.authservice.dto.GroupMemberResponse;
import mexa.club.authservice.dto.GroupRequest;
import mexa.club.authservice.dto.GroupResponse;

import java.util.List;
import java.util.UUID;

public interface GroupService {
    GroupResponse createGroup(GroupRequest request);
    GroupResponse updateGroup(UUID id, GroupRequest request);
    void deleteGroup(UUID id);
    GroupDetailResponse getGroup(UUID id);
    List<GroupResponse> getAllGroups();
    GroupMemberResponse addMember(UUID groupId, AddMemberRequest request);
    void removeMember(UUID groupId, UUID memberId);
}