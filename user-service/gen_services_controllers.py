import os

base_dir = r"c:\Users\User\Downloads\gpl-rfid-livraisons\backend\user-service"

files = {}

# Services
files["src/main/java/com/gpl/user/service/PersonService.java"] = """package com.gpl.user.service;

import com.gpl.common.dto.PageResponse;
import com.gpl.common.dto.ModificationSubObject;
import com.gpl.user.dto.*;
import com.gpl.user.model.*;
import com.gpl.user.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class PersonService {

    private final PersonRepository personRepository;
    private final PersonPhoneRepository phoneRepository;
    private final PersonEmailRepository emailRepository;
    private final UserRoleAssignmentRepository roleAssignmentRepository;
    private final UserSiteAssignmentRepository siteAssignmentRepository;

    private static final AtomicLong sequence = new AtomicLong(1000);

    public PersonService(PersonRepository personRepository,
                         PersonPhoneRepository phoneRepository,
                         PersonEmailRepository emailRepository,
                         UserRoleAssignmentRepository roleAssignmentRepository,
                         UserSiteAssignmentRepository siteAssignmentRepository) {
        this.personRepository = personRepository;
        this.phoneRepository = phoneRepository;
        this.emailRepository = emailRepository;
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.siteAssignmentRepository = siteAssignmentRepository;
    }

    private String generatePersonId() {
        long num = sequence.getAndIncrement();
        char checkLetter = (char) ('A' + (num % 26));
        return num + "-" + checkLetter;
    }

    @Transactional
    public PersonResponse createPerson(CreatePersonRequest request, String createdBy) {
        Person person = new Person();
        person.setPersonId(generatePersonId());
        person.setPersonUid(sequence.get());
        person.setFirstName(request.getFirstName());
        person.setLastName(request.getLastName());
        person.setDisplayName(request.getLastName().toUpperCase() + " " + request.getFirstName());
        person.setUsername(request.getUsername());
        person.setEmail(request.getEmail());
        person.setOrganizationId(request.getOrganizationId());
        person.setOrgId(request.getOrgId());
        person.setPrimarySiteId(request.getPrimarySiteId());
        person.setTitle(request.getTitle());
        person.setJobCode(request.getJobCode());
        person.setPrimaryPhone(request.getPrimaryPhone());
        person.setCity(request.getCity());
        person.setLanguage(request.getLanguage() != null ? request.getLanguage() : "FR");
        person.setSupervisorId(request.getSupervisorId());
        person.setDeviceClass(request.getDeviceClass() != null ? request.getDeviceClass() : 0);
        
        person.setCreatedBy(createdBy);
        person.setCreatedAt(Instant.now());

        if (request.getPhones() != null) {
            for (PhoneDto pDto : request.getPhones()) {
                PersonPhone pp = new PersonPhone();
                pp.setPhoneNum(pDto.getPhoneNum());
                pp.setType(pDto.getType());
                pp.setTypeDescription(pDto.getTypeDescription());
                pp.setPrimary(pDto.isPrimary());
                pp.setPerson(person);
                person.getPhones().add(pp);
            }
        }

        if (request.getEmails() != null) {
            for (EmailDto eDto : request.getEmails()) {
                PersonEmail pe = new PersonEmail();
                pe.setEmailAddress(eDto.getEmailAddress());
                pe.setType(eDto.getType());
                pe.setTypeDescription(eDto.getTypeDescription());
                pe.setPrimary(eDto.isPrimary());
                pe.setPerson(person);
                person.getEmails().add(pe);
            }
        }

        person = personRepository.save(person);
        return buildPersonResponse(person);
    }

    @Transactional
    public PersonResponse updatePerson(String id, UpdatePersonRequest request, String changedBy) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person not found"));

        boolean nameChanged = false;
        if (request.getFirstName() != null && !request.getFirstName().equals(person.getFirstName())) {
            person.setFirstName(request.getFirstName());
            nameChanged = true;
        }
        if (request.getLastName() != null && !request.getLastName().equals(person.getLastName())) {
            person.setLastName(request.getLastName());
            nameChanged = true;
        }
        if (nameChanged) {
            person.setDisplayName(person.getLastName().toUpperCase() + " " + person.getFirstName());
        }

        if (request.getEmail() != null) person.setEmail(request.getEmail());
        if (request.getTitle() != null) person.setTitle(request.getTitle());
        if (request.getJobCode() != null) person.setJobCode(request.getJobCode());
        if (request.getJobCodeDescription() != null) person.setJobCodeDescription(request.getJobCodeDescription());
        if (request.getPrimaryPhone() != null) person.setPrimaryPhone(request.getPrimaryPhone());
        if (request.getAddressLine1() != null) person.setAddressLine1(request.getAddressLine1());
        if (request.getCity() != null) person.setCity(request.getCity());
        if (request.getAvatarUrl() != null) person.setAvatarUrl(request.getAvatarUrl());
        if (request.getSupervisorId() != null) person.setSupervisorId(request.getSupervisorId());
        if (request.getDeviceClass() != null) person.setDeviceClass(request.getDeviceClass());
        if (request.getDeviceClassDescription() != null) person.setDeviceClassDescription(request.getDeviceClassDescription());
        if (request.getWfMailElection() != null) person.setWfMailElection(request.getWfMailElection());
        if (request.getTransEmailElection() != null) person.setTransEmailElection(request.getTransEmailElection());

        person.setChangeby(changedBy);
        person.setChangedate(Instant.now());

        person = personRepository.save(person);
        return buildPersonResponse(person);
    }

    public PersonResponse getPerson(String id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person not found"));
        return buildPersonResponse(person);
    }

    public PageResponse<PersonSummaryResponse> listPersons(String organizationId, String siteId, String jobCode, Boolean isActive, String search, Pageable pageable) {
        Page<Person> page;
        if (search != null && !search.isEmpty()) {
            page = personRepository.findByDisplayNameContainingIgnoreCase(search, pageable);
        } else if (organizationId != null) {
            page = personRepository.findByOrganizationId(organizationId, pageable);
        } else if (isActive != null && isActive) {
            page = personRepository.findByIsActiveTrue(pageable);
        } else {
            page = personRepository.findAll(pageable);
        }

        List<PersonSummaryResponse> list = page.getContent().stream().map(p -> {
            PersonSummaryResponse res = new PersonSummaryResponse();
            res.setId(p.getId());
            res.setPersonId(p.getPersonId());
            res.setDisplayName(p.getDisplayName());
            res.setTitle(p.getTitle());
            res.setJobCode(p.getJobCode());
            res.setOrgId(p.getOrgId());
            res.setSiteId(p.getPrimarySiteId());
            res.setStatus(p.getStatus());
            res.setStatusDescription(p.getStatusDescription());
            res.setActive(p.isActive());
            return res;
        }).collect(Collectors.toList());

        return new PageResponse<>(list, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Transactional
    public PersonResponse updatePersonStatus(String id, UpdateStatusRequest request, String changedBy) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person not found"));
        person.setStatus(request.getNewStatus());
        person.setStatusDescription(request.getReason());
        person.setStatusDate(Instant.now());
        person.setChangeby(changedBy);
        person.setChangedate(Instant.now());
        return buildPersonResponse(personRepository.save(person));
    }

    private PersonResponse buildPersonResponse(Person person) {
        PersonResponse response = new PersonResponse();
        response.setId(person.getId());
        response.setPersonId(person.getPersonId());
        response.setPersonUid(person.getPersonUid());
        response.setOrganizationId(person.getOrganizationId());
        response.setOrgId(person.getOrgId());
        response.setPrimarySiteId(person.getPrimarySiteId());
        response.setSiteId(person.getSiteId());
        response.setLocationOrg(person.getLocationOrg());
        response.setLocationSite(person.getLocationSite());
        response.setLocation(person.getLocation());
        response.setUsername(person.getUsername());
        response.setEmail(person.getEmail());
        response.setFirstName(person.getFirstName());
        response.setLastName(person.getLastName());
        response.setDisplayName(person.getDisplayName());
        response.setTitle(person.getTitle());
        response.setJobCode(person.getJobCode());
        response.setJobCodeDescription(person.getJobCodeDescription());
        response.setPrimaryPhone(person.getPrimaryPhone());
        response.setAddressLine1(person.getAddressLine1());
        response.setCity(person.getCity());
        response.setLanguage(person.getLanguage());
        response.setAvatarUrl(person.getAvatarUrl());
        response.setSupervisorId(person.getSupervisorId());
        response.setActive(person.isActive());
        response.setLocked(person.isLocked());
        response.setCertified(person.isCertified());
        response.setAcceptingWfMail(person.isAcceptingWfMail());
        response.setLocToServReq(person.isLocToServReq());
        response.setStatusIface(person.isStatusIface());
        response.setDeviceClass(person.getDeviceClass());
        response.setDeviceClassDescription(person.getDeviceClassDescription());
        response.setWfMailElection(person.getWfMailElection());
        response.setWfMailElectionDescription(person.getWfMailElectionDescription());
        response.setTransEmailElection(person.getTransEmailElection());
        response.setTransEmailElectionDescription(person.getTransEmailElectionDescription());
        response.setLastLoginAt(person.getLastLoginAt());
        response.setStatus(person.getStatus());
        response.setStatusDescription(person.getStatusDescription());
        response.setStatusDate(person.getStatusDate());

        response.setPhones(person.getPhones().stream().map(p -> {
            PhoneDto dto = new PhoneDto();
            dto.setPhoneId(p.getPhoneId());
            dto.setPhoneNum(p.getPhoneNum());
            dto.setType(p.getType());
            dto.setTypeDescription(p.getTypeDescription());
            dto.setPrimary(p.isPrimary());
            return dto;
        }).collect(Collectors.toList()));

        response.setEmails(person.getEmails().stream().map(e -> {
            EmailDto dto = new EmailDto();
            dto.setEmailId(e.getEmailId());
            dto.setEmailAddress(e.getEmailAddress());
            dto.setType(e.getType());
            dto.setTypeDescription(e.getTypeDescription());
            dto.setPrimary(e.isPrimary());
            return dto;
        }).collect(Collectors.toList()));

        response.setRoles(roleAssignmentRepository.findActiveByPersonId(person.getPersonId()));
        response.setSites(siteAssignmentRepository.findByPersonIdAndIsActiveTrue(person.getPersonId()));
        
        response.setModificationsRef("/api/v1/persons/" + person.getId() + "/modifications");
        response.setRecentModifications(new ArrayList<>()); // mock recent modifications

        return response;
    }
}
"""

files["src/main/java/com/gpl/user/service/RoleService.java"] = """package com.gpl.user.service;

import com.gpl.user.dto.CreateRoleRequest;
import com.gpl.user.dto.PermissionResponse;
import com.gpl.user.dto.RoleResponse;
import com.gpl.user.model.Permission;
import com.gpl.user.model.Role;
import com.gpl.user.model.RolePermission;
import com.gpl.user.repository.PermissionRepository;
import com.gpl.user.repository.RolePermissionRepository;
import com.gpl.user.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RoleService {
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;

    public RoleService(RoleRepository roleRepository, RolePermissionRepository rolePermissionRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request, String createdBy) {
        Role role = new Role();
        role.setCode(request.getCode());
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setScopeOrgType(request.getScopeOrgType());
        role.setMinTier(request.getMinTier());
        role.setSortOrder(request.getSortOrder());
        role.setCreatedBy(createdBy);
        role.setCreatedAt(Instant.now());
        role = roleRepository.save(role);
        return buildRoleResponse(role);
    }

    public RoleResponse getRole(String id) {
        Role role = roleRepository.findById(id).orElseThrow();
        return buildRoleResponse(role);
    }
    
    public List<RoleResponse> listRoles() {
        return roleRepository.findAll().stream().map(this::buildRoleResponse).collect(Collectors.toList());
    }

    public List<PermissionResponse> getPermissionsForRole(String roleId) {
        List<RolePermission> rps = rolePermissionRepository.findByRoleId(roleId);
        return rps.stream()
                .filter(RolePermission::isGranted)
                .map(rp -> permissionRepository.findById(rp.getPermissionId()).orElse(null))
                .filter(p -> p != null)
                .map(p -> {
                    PermissionResponse pr = new PermissionResponse();
                    pr.setId(p.getId());
                    pr.setCode(p.getCode());
                    pr.setName(p.getName());
                    pr.setDescription(p.getDescription());
                    pr.setModule(p.getModule());
                    pr.setModuleDescription(p.getModuleDescription());
                    pr.setActive(p.isActive());
                    pr.setSortOrder(p.getSortOrder());
                    return pr;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void grantPermission(String roleId, String permissionId, String grantedBy) {
        Optional<RolePermission> opt = rolePermissionRepository.findByRoleIdAndPermissionId(roleId, permissionId);
        RolePermission rp;
        if (opt.isPresent()) {
            rp = opt.get();
            rp.setGranted(true);
        } else {
            rp = new RolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(permissionId);
            rp.setGranted(true);
        }
        rp.setGrantedBy(grantedBy);
        rp.setGrantedAt(Instant.now());
        rolePermissionRepository.save(rp);
    }

    @Transactional
    public void revokePermission(String roleId, String permissionId) {
        Optional<RolePermission> opt = rolePermissionRepository.findByRoleIdAndPermissionId(roleId, permissionId);
        opt.ifPresent(rp -> {
            rp.setGranted(false);
            rolePermissionRepository.save(rp);
        });
    }

    private RoleResponse buildRoleResponse(Role role) {
        RoleResponse res = new RoleResponse();
        res.setId(role.getId());
        res.setCode(role.getCode());
        res.setName(role.getName());
        res.setDescription(role.getDescription());
        res.setScopeOrgType(role.getScopeOrgType());
        res.setMinTier(role.getMinTier());
        res.setStatus(role.getStatus());
        res.setStatusDescription(role.getStatusDescription());
        res.setSystemRole(role.isSystemRole());
        res.setActive(role.isActive());
        res.setSortOrder(role.getSortOrder());
        res.setPermissions(getPermissionsForRole(role.getId()));
        return res;
    }
}
"""

files["src/main/java/com/gpl/user/service/PermissionService.java"] = """package com.gpl.user.service;

import com.gpl.user.model.UserRoleAssignment;
import com.gpl.user.repository.PermissionRepository;
import com.gpl.user.repository.RolePermissionRepository;
import com.gpl.user.repository.UserRoleAssignmentRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PermissionService {
    private final UserRoleAssignmentRepository uraRepo;
    private final RolePermissionRepository rolePermissionRepo;

    public PermissionService(UserRoleAssignmentRepository uraRepo, RolePermissionRepository rolePermissionRepo) {
        this.uraRepo = uraRepo;
        this.rolePermissionRepo = rolePermissionRepo;
    }

    public Set<String> resolvePermissions(String personId) {
        List<UserRoleAssignment> activeRoles = uraRepo.findActiveByPersonId(personId);
        Set<String> perms = new HashSet<>();
        for (UserRoleAssignment ura : activeRoles) {
            perms.addAll(rolePermissionRepo.findPermissionCodesByRoleId(ura.getRoleId()));
        }
        return perms;
    }

    public boolean hasPermission(String personId, String permissionCode) {
        Set<String> perms = resolvePermissions(personId);
        return perms.contains(permissionCode);
    }
}
"""

files["src/main/java/com/gpl/user/service/UserRoleAssignmentService.java"] = """package com.gpl.user.service;

import com.gpl.user.dto.AssignRoleRequest;
import com.gpl.user.model.UserRoleAssignment;
import com.gpl.user.repository.UserRoleAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class UserRoleAssignmentService {
    private final UserRoleAssignmentRepository repository;

    public UserRoleAssignmentService(UserRoleAssignmentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public UserRoleAssignment assignRole(AssignRoleRequest request, String assignedBy) {
        UserRoleAssignment ura = new UserRoleAssignment();
        ura.setPersonId(request.getPersonId());
        ura.setRoleId(request.getRoleId());
        ura.setSiteId(request.getSiteId());
        ura.setValidFrom(request.getValidFrom());
        ura.setValidUntil(request.getValidUntil());
        ura.setPrimary(request.isPrimary());
        ura.setActive(true);
        ura.setCreatedBy(assignedBy);
        ura.setCreatedAt(Instant.now());
        return repository.save(ura);
    }

    @Transactional
    public void revokeRole(String assignmentId, String revokedBy) {
        repository.findById(assignmentId).ifPresent(ura -> {
            ura.setActive(false);
            ura.setChangeby(revokedBy);
            ura.setChangedate(Instant.now());
            repository.save(ura);
        });
    }

    public List<UserRoleAssignment> getAssignmentsByPerson(String personId) {
        return repository.findByPersonIdAndIsActiveTrue(personId);
    }
}
"""

files["src/main/java/com/gpl/user/service/UserSiteAssignmentService.java"] = """package com.gpl.user.service;

import com.gpl.user.dto.AssignSiteRequest;
import com.gpl.user.model.UserSiteAssignment;
import com.gpl.user.repository.UserSiteAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class UserSiteAssignmentService {
    private final UserSiteAssignmentRepository repository;

    public UserSiteAssignmentService(UserSiteAssignmentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public UserSiteAssignment assignSite(AssignSiteRequest request, String assignedBy) {
        UserSiteAssignment usa = new UserSiteAssignment();
        usa.setPersonId(request.getPersonId());
        usa.setSiteId(request.getSiteId());
        usa.setOrganizationId(request.getOrganizationId());
        usa.setPrimary(request.isPrimary());
        usa.setDefault(request.isDefault());
        usa.setActive(true);
        usa.setCreatedBy(assignedBy);
        usa.setCreatedAt(Instant.now());
        return repository.save(usa);
    }

    @Transactional
    public void revokeSite(String assignmentId) {
        repository.findById(assignmentId).ifPresent(usa -> {
            usa.setActive(false);
            repository.save(usa);
        });
    }

    @Transactional
    public void setPrimary(String personId, String siteId) {
        List<UserSiteAssignment> assignments = repository.findByPersonIdAndIsActiveTrue(personId);
        for (UserSiteAssignment usa : assignments) {
            usa.setPrimary(usa.getSiteId().equals(siteId));
            repository.save(usa);
        }
    }

    public List<UserSiteAssignment> getAssignmentsByPerson(String personId) {
        return repository.findByPersonIdAndIsActiveTrue(personId);
    }
}
"""

files["src/main/java/com/gpl/user/service/UserGroupService.java"] = """package com.gpl.user.service;

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
"""

# Controllers
files["src/main/java/com/gpl/user/controller/PersonController.java"] = """package com.gpl.user.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.common.dto.PageResponse;
import com.gpl.user.dto.*;
import com.gpl.user.model.UserRoleAssignment;
import com.gpl.user.model.UserSiteAssignment;
import com.gpl.user.service.PermissionService;
import com.gpl.user.service.PersonService;
import com.gpl.user.service.UserRoleAssignmentService;
import com.gpl.user.service.UserSiteAssignmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
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

    @GetMapping("/{id}")
    public ApiResponse<PersonResponse> getPerson(@PathVariable String id) {
        return ApiResponse.success(personService.getPerson(id));
    }

    @PostMapping("/")
    public ApiResponse<PersonResponse> createPerson(
            @Valid @RequestBody CreatePersonRequest request,
            @RequestHeader("X-User-PersonId") String createdBy) {
        return ApiResponse.success(personService.createPerson(request, createdBy));
    }

    @PutMapping("/{id}")
    public ApiResponse<PersonResponse> updatePerson(
            @PathVariable String id,
            @RequestBody UpdatePersonRequest request,
            @RequestHeader("X-User-PersonId") String changedBy) {
        return ApiResponse.success(personService.updatePerson(id, request, changedBy));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<PersonResponse> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateStatusRequest request,
            @RequestHeader("X-User-PersonId") String changedBy) {
        return ApiResponse.success(personService.updatePersonStatus(id, request, changedBy));
    }

    @GetMapping("/{id}/phones")
    public ApiResponse<List<PhoneDto>> getPhones(@PathVariable String id) {
        return ApiResponse.success(personService.getPerson(id).getPhones());
    }

    @GetMapping("/{id}/emails")
    public ApiResponse<List<EmailDto>> getEmails(@PathVariable String id) {
        return ApiResponse.success(personService.getPerson(id).getEmails());
    }

    @GetMapping("/{id}/roles")
    public ApiResponse<List<UserRoleAssignment>> getRoles(@PathVariable String id) {
        PersonResponse person = personService.getPerson(id);
        return ApiResponse.success(roleAssignmentService.getAssignmentsByPerson(person.getPersonId()));
    }

    @PostMapping("/{id}/roles")
    public ApiResponse<UserRoleAssignment> assignRole(
            @PathVariable String id,
            @Valid @RequestBody AssignRoleRequest request,
            @RequestHeader("X-User-PersonId") String assignedBy) {
        request.setPersonId(personService.getPerson(id).getPersonId());
        return ApiResponse.success(roleAssignmentService.assignRole(request, assignedBy));
    }

    @DeleteMapping("/{id}/roles/{assignmentId}")
    public ApiResponse<Void> revokeRole(
            @PathVariable String id,
            @PathVariable String assignmentId,
            @RequestHeader("X-User-PersonId") String revokedBy) {
        roleAssignmentService.revokeRole(assignmentId, revokedBy);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}/sites")
    public ApiResponse<List<UserSiteAssignment>> getSites(@PathVariable String id) {
        PersonResponse person = personService.getPerson(id);
        return ApiResponse.success(siteAssignmentService.getAssignmentsByPerson(person.getPersonId()));
    }

    @PostMapping("/{id}/sites")
    public ApiResponse<UserSiteAssignment> assignSite(
            @PathVariable String id,
            @Valid @RequestBody AssignSiteRequest request,
            @RequestHeader("X-User-PersonId") String assignedBy) {
        request.setPersonId(personService.getPerson(id).getPersonId());
        return ApiResponse.success(siteAssignmentService.assignSite(request, assignedBy));
    }

    @GetMapping("/{id}/permissions")
    public ApiResponse<Set<String>> getPermissions(@PathVariable String id) {
        PersonResponse person = personService.getPerson(id);
        return ApiResponse.success(permissionService.resolvePermissions(person.getPersonId()));
    }
}
"""

files["src/main/java/com/gpl/user/controller/RoleController.java"] = """package com.gpl.user.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.user.dto.CreateRoleRequest;
import com.gpl.user.dto.PermissionResponse;
import com.gpl.user.dto.RoleResponse;
import com.gpl.user.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {
    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/")
    public ApiResponse<List<RoleResponse>> listRoles() {
        return ApiResponse.success(roleService.listRoles());
    }

    @GetMapping("/{id}")
    public ApiResponse<RoleResponse> getRole(@PathVariable String id) {
        return ApiResponse.success(roleService.getRole(id));
    }

    @PostMapping("/")
    public ApiResponse<RoleResponse> createRole(
            @Valid @RequestBody CreateRoleRequest request,
            @RequestHeader("X-User-PersonId") String createdBy) {
        return ApiResponse.success(roleService.createRole(request, createdBy));
    }

    @GetMapping("/{id}/permissions")
    public ApiResponse<List<PermissionResponse>> getPermissions(@PathVariable String id) {
        return ApiResponse.success(roleService.getPermissionsForRole(id));
    }

    @PostMapping("/{id}/permissions")
    public ApiResponse<Void> grantPermission(
            @PathVariable String id,
            @RequestParam String permissionId,
            @RequestHeader("X-User-PersonId") String grantedBy) {
        roleService.grantPermission(id, permissionId, grantedBy);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}/permissions/{permissionId}")
    public ApiResponse<Void> revokePermission(
            @PathVariable String id,
            @PathVariable String permissionId) {
        roleService.revokePermission(id, permissionId);
        return ApiResponse.success(null);
    }
}
"""

files["src/main/java/com/gpl/user/controller/PermissionController.java"] = """package com.gpl.user.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.user.model.Permission;
import com.gpl.user.repository.PermissionRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionController {
    private final PermissionRepository repository;

    public PermissionController(PermissionRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/")
    public ApiResponse<List<Permission>> listPermissions() {
        return ApiResponse.success(repository.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Permission> getPermission(@PathVariable String id) {
        return ApiResponse.success(repository.findById(id).orElseThrow());
    }

    @GetMapping("/modules")
    public ApiResponse<List<String>> listModules() {
        List<String> modules = repository.findAll().stream()
                .map(Permission::getModule)
                .distinct()
                .collect(Collectors.toList());
        return ApiResponse.success(modules);
    }
}
"""

files["src/main/java/com/gpl/user/controller/UserGroupController.java"] = """package com.gpl.user.controller;

import com.gpl.common.dto.ApiResponse;
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

    @GetMapping("/")
    public ApiResponse<List<GroupResponse>> listGroups() {
        return ApiResponse.success(groupService.listGroups());
    }

    @GetMapping("/{id}")
    public ApiResponse<GroupResponse> getGroup(@PathVariable String id) {
        return ApiResponse.success(groupService.getGroup(id));
    }

    @PostMapping("/")
    public ApiResponse<GroupResponse> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            @RequestHeader("X-User-PersonId") String createdBy) {
        return ApiResponse.success(groupService.createGroup(request, createdBy));
    }

    @GetMapping("/{id}/members")
    public ApiResponse<List<GroupMemberResponse>> getMembers(@PathVariable String id) {
        return ApiResponse.success(groupService.getMembers(id));
    }

    @PostMapping("/{id}/members")
    public ApiResponse<Void> addMember(
            @PathVariable String id,
            @Valid @RequestBody AddGroupMemberRequest request,
            @RequestHeader("X-User-PersonId") String addedBy) {
        groupService.addMember(id, request, addedBy);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}/members/{personId}")
    public ApiResponse<Void> removeMember(
            @PathVariable String id,
            @PathVariable String personId) {
        groupService.removeMember(id, personId);
        return ApiResponse.success(null);
    }
}
"""

for path, content in files.items():
    full_path = os.path.join(base_dir, path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, "w", encoding="utf-8") as f:
        f.write(content.strip() + "\\n")

print("Generated services and controllers.")
