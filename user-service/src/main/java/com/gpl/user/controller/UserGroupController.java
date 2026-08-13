package com.gpl.user.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.common.security.RequiresPermission;
import com.gpl.user.dto.AddGroupMemberRequest;
import com.gpl.user.dto.CreateGroupRequest;
import com.gpl.user.dto.GroupMemberResponse;
import com.gpl.user.dto.GroupResponse;
import com.gpl.user.service.UserGroupService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups")
public class UserGroupController {
    private final UserGroupService groupService;

    public UserGroupController(UserGroupService groupService) {
        this.groupService = groupService;
    }

    @RequiresPermission("GROUP_VIEW")
    @GetMapping("/")
    public ApiResponse<List<GroupResponse>> listGroups() {
        return ApiResponse.success(groupService.listGroups());
    }

    @RequiresPermission("GROUP_VIEW")
    @GetMapping("/{id}")
    public ApiResponse<GroupResponse> getGroup(@PathVariable String id) {
        return ApiResponse.success(groupService.getGroup(id));
    }

    @RequiresPermission("GROUP_CREATE")
    @PostMapping("/")
    public ApiResponse<GroupResponse> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            @RequestHeader("X-User-PersonId") String createdBy) {
        return ApiResponse.success(groupService.createGroup(request, createdBy));
    }

    @RequiresPermission("GROUP_VIEW")
    @GetMapping("/{id}/members")
    public ApiResponse<List<GroupMemberResponse>> getMembers(@PathVariable String id) {
        return ApiResponse.success(groupService.getMembers(id));
    }

    @RequiresPermission("GROUP_ADD_MEMBER")
    @PostMapping("/{id}/members")
    public ApiResponse<Void> addMember(
            @PathVariable String id,
            @Valid @RequestBody AddGroupMemberRequest request,
            @RequestHeader("X-User-PersonId") String addedBy) {
        groupService.addMember(id, request, addedBy);
        return ApiResponse.success(null);
    }

    @RequiresPermission("GROUP_REMOVE_MEMBER")
    @DeleteMapping("/{id}/members/{personId}")
    public ApiResponse<Void> removeMember(
            @PathVariable String id,
            @PathVariable String personId) {
        groupService.removeMember(id, personId);
        return ApiResponse.success(null);
    }
}
