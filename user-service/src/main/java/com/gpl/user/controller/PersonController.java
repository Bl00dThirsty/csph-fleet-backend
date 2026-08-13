package com.gpl.user.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.common.dto.PageResponse;
import com.gpl.common.security.RequiresPermission;
import com.gpl.user.dto.*;
import com.gpl.user.model.UserRoleAssignment;
import com.gpl.user.model.UserSiteAssignment;
import com.gpl.user.service.PermissionService;
import com.gpl.user.service.PersonService;
import com.gpl.user.service.UserRoleAssignmentService;
import com.gpl.user.service.UserSiteAssignmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/persons")
public class PersonController {

    private final PersonService personService;
    private final UserRoleAssignmentService roleAssignmentService;
    private final UserSiteAssignmentService siteAssignmentService;
    private final PermissionService permissionService;

    public PersonController(PersonService personService, UserRoleAssignmentService roleAssignmentService, UserSiteAssignmentService siteAssignmentService, PermissionService permissionService) {
        this.personService = personService;
        this.roleAssignmentService = roleAssignmentService;
        this.siteAssignmentService = siteAssignmentService;
        this.permissionService = permissionService;
    }

    @RequiresPermission("PERSON_VIEW")
    @GetMapping("/")
    public PageResponse<PersonSummaryResponse> listPersons(
            @RequestParam(required = false) String organizationId,
            @RequestParam(required = false) String siteId,
            @RequestParam(required = false) String jobCode,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return personService.listPersons(organizationId, siteId, jobCode, isActive, search, PageRequest.of(page, size));
    }

    @RequiresPermission("PERSON_VIEW")
    @GetMapping("/{id}")
    public ApiResponse<PersonResponse> getPerson(@PathVariable String id) {
        return ApiResponse.success(personService.getPerson(id));
    }

    @GetMapping("/by-person-id/{personId}")
    public ResponseEntity<ApiResponse<PersonResponse>> getPersonByPersonId(@PathVariable String personId) {
        return ResponseEntity.ok(ApiResponse.success(personService.getPersonByPersonId(personId)));
    }

    @RequiresPermission("PERSON_CREATE")
    @PostMapping("/")
    public ApiResponse<PersonResponse> createPerson(
            @Valid @RequestBody CreatePersonRequest request,
            @RequestHeader("X-User-PersonId") String createdBy) {
        return ApiResponse.success(personService.createPerson(request, createdBy));
    }

    @RequiresPermission("PERSON_UPDATE")
    @PutMapping("/{id}")
    public ApiResponse<PersonResponse> updatePerson(
            @PathVariable String id,
            @RequestBody UpdatePersonRequest request,
            @RequestHeader("X-User-PersonId") String changedBy) {
        return ApiResponse.success(personService.updatePerson(id, request, changedBy));
    }

    @RequiresPermission("PERSON_UPDATE")
    @PatchMapping("/{id}/status")
    public ApiResponse<PersonResponse> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateStatusRequest request,
            @RequestHeader("X-User-PersonId") String changedBy) {
        return ApiResponse.success(personService.updatePersonStatus(id, request, changedBy));
    }

    @RequiresPermission("PERSON_VIEW")
    @GetMapping("/{id}/phones")
    public ApiResponse<List<PhoneDto>> getPhones(@PathVariable String id) {
        return ApiResponse.success(personService.getPerson(id).getPhones());
    }

    @RequiresPermission("PERSON_VIEW")
    @GetMapping("/{id}/emails")
    public ApiResponse<List<EmailDto>> getEmails(@PathVariable String id) {
        return ApiResponse.success(personService.getPerson(id).getEmails());
    }

    @RequiresPermission("ROLE_VIEW")
    @GetMapping("/{id}/roles")
    public ApiResponse<List<UserRoleAssignment>> getRoles(@PathVariable String id) {
        PersonResponse person = personService.getPerson(id);
        return ApiResponse.success(roleAssignmentService.getAssignmentsByPerson(person.getPersonId()));
    }

    @RequiresPermission("ROLE_UPDATE")
    @PostMapping("/{id}/roles")
    public ApiResponse<UserRoleAssignment> assignRole(
            @PathVariable String id,
            @Valid @RequestBody AssignRoleRequest request,
            @RequestHeader("X-User-PersonId") String assignedBy) {
        request.setPersonId(personService.getPerson(id).getPersonId());
        return ApiResponse.success(roleAssignmentService.assignRole(request, assignedBy));
    }

    @RequiresPermission("ROLE_UPDATE")
    @DeleteMapping("/{id}/roles/{assignmentId}")
    public ApiResponse<Void> revokeRole(
            @PathVariable String id,
            @PathVariable String assignmentId,
            @RequestHeader("X-User-PersonId") String revokedBy) {
        roleAssignmentService.revokeRole(assignmentId, revokedBy);
        return ApiResponse.success(null);
    }

    @RequiresPermission("PERSON_VIEW")
    @GetMapping("/{id}/sites")
    public ApiResponse<List<UserSiteAssignment>> getSites(@PathVariable String id) {
        PersonResponse person = personService.getPerson(id);
        return ApiResponse.success(siteAssignmentService.getAssignmentsByPerson(person.getPersonId()));
    }

    @RequiresPermission("PERSON_UPDATE")
    @PostMapping("/{id}/sites")
    public ApiResponse<UserSiteAssignment> assignSite(
            @PathVariable String id,
            @Valid @RequestBody AssignSiteRequest request,
            @RequestHeader("X-User-PersonId") String assignedBy) {
        request.setPersonId(personService.getPerson(id).getPersonId());
        return ApiResponse.success(siteAssignmentService.assignSite(request, assignedBy));
    }

    @RequiresPermission("PERMISSION_VIEW")
    @GetMapping("/{id}/permissions")
    public ApiResponse<Set<String>> getPermissions(@PathVariable String id) {
        PersonResponse person = personService.getPerson(id);
        return ApiResponse.success(permissionService.resolvePermissions(person.getPersonId()));
    }

    @GetMapping("/by-person-id/{personId}/permissions")
    public ResponseEntity<ApiResponse<Set<String>>> getPermissionsByPersonId(@PathVariable String personId) {
        return ResponseEntity.ok(ApiResponse.success(permissionService.resolvePermissions(personId)));
    }
}
