package com.gpl.user.service;

import com.gpl.common.dto.PageResponse;
import com.gpl.user.dto.AddGroupMemberRequest;
import com.gpl.user.dto.CreateGroupRequest;
import com.gpl.user.dto.GroupMemberResponse;
import com.gpl.user.dto.GroupResponse;
import com.gpl.user.model.Person;
import com.gpl.user.model.UserGroup;
import com.gpl.user.model.UserGroupMembership;
import com.gpl.user.repository.PersonRepository;
import com.gpl.user.repository.UserGroupMembershipRepository;
import com.gpl.user.repository.UserGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserGroupService {
    private final UserGroupRepository groupRepository;
    private final UserGroupMembershipRepository membershipRepository;
    private final PersonRepository personRepository;

    public UserGroupService(UserGroupRepository groupRepository, UserGroupMembershipRepository membershipRepository, PersonRepository personRepository) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.personRepository = personRepository;
    }

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request, String createdBy) {
        UserGroup group = new UserGroup();
        group.setCode(request.getCode());
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setOrganizationId(request.getOrganizationId());
        group.setSiteId(request.getSiteId());
        group.setActive(true);
        group.setCreatedBy(createdBy);
        group.setCreatedAt(Instant.now());
        group = groupRepository.save(group);
        return buildGroupResponse(group);
    }

    public GroupResponse getGroup(String id) {
        return buildGroupResponse(groupRepository.findById(id).orElseThrow());
    }
    
    public List<GroupResponse> listGroups() {
        return groupRepository.findAll().stream().map(this::buildGroupResponse).collect(Collectors.toList());
    }

    @Transactional
    public void addMember(String groupId, AddGroupMemberRequest request, String addedBy) {
        if (!membershipRepository.existsByPersonIdAndGroupId(request.getPersonId(), groupId)) {
            UserGroupMembership membership = new UserGroupMembership();
            membership.setGroupId(groupId);
            membership.setPersonId(request.getPersonId());
            membership.setAddedBy(addedBy);
            membership.setActive(true);
            membershipRepository.save(membership);
            incrementMemberCount(groupId);
        }
    }

    @Transactional
    public void removeMember(String groupId, String personId) {
        Optional<UserGroupMembership> opt = membershipRepository.findByPersonIdAndGroupId(personId, groupId);
        opt.ifPresent(m -> {
            membershipRepository.delete(m);
            decrementMemberCount(groupId);
        });
    }

    public List<GroupMemberResponse> getMembers(String groupId) {
        return membershipRepository.findByGroupIdAndIsActiveTrue(groupId).stream().map(m -> {
            GroupMemberResponse r = new GroupMemberResponse();
            r.setPersonId(m.getPersonId());
            personRepository.findByPersonId(m.getPersonId()).ifPresent(p -> r.setDisplayName(p.getDisplayName()));
            r.setJoinedAt(m.getJoinedAt());
            r.setAddedBy(m.getAddedBy());
            r.setActive(m.isActive());
            return r;
        }).collect(Collectors.toList());
    }

    private void incrementMemberCount(String groupId) {
        groupRepository.findById(groupId).ifPresent(g -> {
            g.setMemberCount(g.getMemberCount() + 1);
            groupRepository.save(g);
        });
    }

    private void decrementMemberCount(String groupId) {
        groupRepository.findById(groupId).ifPresent(g -> {
            g.setMemberCount(Math.max(0, g.getMemberCount() - 1));
            groupRepository.save(g);
        });
    }

    @Transactional
    public void deleteGroup(String id) {
        groupRepository.deleteById(id);
    }

    private GroupResponse buildGroupResponse(UserGroup group) {
        GroupResponse res = new GroupResponse();
        res.setId(group.getId());
        res.setCode(group.getCode());
        res.setName(group.getName());
        res.setDescription(group.getDescription());
        res.setOrganizationId(group.getOrganizationId());
        res.setSiteId(group.getSiteId());
        res.setActive(group.isActive());
        res.setSystemGroup(group.isSystemGroup());
        res.setMemberCount(group.getMemberCount());
        return res;
    }
}
