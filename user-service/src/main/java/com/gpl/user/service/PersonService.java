package com.gpl.user.service;

import com.gpl.common.dto.PageResponse;
import com.gpl.common.enums.EntityStatus;
import com.gpl.user.client.AuthClient;
import com.gpl.user.dto.*;
import com.gpl.user.model.Person;
import com.gpl.user.model.PersonEmail;
import com.gpl.user.model.PersonPhone;
import com.gpl.user.model.Role;
import com.gpl.user.model.UserRoleAssignment;
import com.gpl.user.repository.PersonEmailRepository;
import com.gpl.user.repository.PersonPhoneRepository;
import com.gpl.user.repository.PersonRepository;
import com.gpl.user.repository.RoleRepository;
import com.gpl.user.repository.UserRoleAssignmentRepository;
import com.gpl.user.repository.UserSiteAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Service for managing persons, their assignments, and authentication provisioning.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PersonService {

    private final PersonRepository personRepository;
    private final PersonPhoneRepository phoneRepository;
    private final PersonEmailRepository emailRepository;
    private final UserRoleAssignmentRepository roleAssignmentRepository;
    private final UserSiteAssignmentRepository siteAssignmentRepository;
    private final RoleRepository roleRepository;
    private final AuthClient authClient;

    private static final AtomicLong PERSON_SEQ = new AtomicLong(5000);

    @Transactional
    public PersonResponse createPerson(CreatePersonRequest request, String createdBy) {

        Person person = new Person();
        person.setPersonId(generatePersonId());
        person.setPersonUid(PERSON_SEQ.incrementAndGet());
        person.setOrganizationId(request.getOrganizationId());
        person.setOrgId(request.getOrgId() != null ? request.getOrgId() : request.getOrganizationId());
        person.setPrimarySiteId(request.getPrimarySiteId());
        person.setSiteId(request.getPrimarySiteId());
        person.setLocationOrg(request.getOrgId());
        person.setLocationSite(request.getPrimarySiteId());

        person.setEmail(request.getEmail());
        person.setFirstName(request.getFirstName());
        person.setLastName(request.getLastName());
        person.setDisplayName(request.getLastName().toUpperCase() + " " + request.getFirstName());
        person.setTitle(request.getTitle());
        person.setJobCode(request.getJobCode());
        person.setPrimaryPhone(request.getPrimaryPhone());
        person.setCity(request.getCity());
        person.setLanguage(request.getLanguage() != null ? request.getLanguage() : "FR");
        person.setSupervisorId(request.getSupervisorId());
        person.setDeviceClass(request.getDeviceClass() != null ? request.getDeviceClass() : 0);

        person.updateStatus(EntityStatus.ACTIVE.getCode(), EntityStatus.ACTIVE.getDescription());
        person.setCreatedBy(createdBy != null ? createdBy : "SYSTEM");

        Person saved = personRepository.save(person);

        if (request.getPhones() != null) {
            int pId = 100;
            for (PhoneDto phoneDto : request.getPhones()) {
                PersonPhone phone = new PersonPhone();
                phone.setPhoneId(pId++);
                phone.setPersonId(saved.getPersonId());
                phone.setPhoneNum(phoneDto.getPhoneNum());
                phone.setType(phoneDto.getType());
                phone.setTypeDescription(phoneDto.getTypeDescription());
                phone.setPrimary(phoneDto.isPrimary());
                phoneRepository.save(phone);
            }
        }

        if (request.getEmails() != null) {
            int eId = 100;
            for (EmailDto emailDto : request.getEmails()) {
                PersonEmail email = new PersonEmail();
                email.setEmailId(eId++);
                email.setPersonId(saved.getPersonId());
                email.setEmailAddress(emailDto.getEmailAddress());
                email.setType(emailDto.getType());
                email.setTypeDescription(emailDto.getTypeDescription());
                email.setPrimary(emailDto.isPrimary());
                emailRepository.save(email);
            }
        }

        return getPerson(saved.getId());
    }

    /**
     * Creates a new person and provisions authentication credentials.
     * Used for creating users who need login access (e.g. drivers/livreurs).
     *
     * @param request the creation request including auth credentials
     * @param createdBy the identifier of the user creating this person
     * @return the created person response
     */
    /**
     * Maps a role coming from a client onto a seeded role code.
     *
     * <p>The web client keeps French UI names ({@code LIVREUR}, {@code MARKETEUR},
     * {@code TRANSPORTEUR}) while the seeded catalogue in
     * {@code RolesPermissionsInitializer} uses {@code DRIVER}, {@code MARKETER}
     * and {@code TRANSPORTER}. Without this the lookup below would 404 on every
     * non-English caller and the person would end up with no role at all.
     * Anything already matching a seeded code is passed through untouched.
     */
    private String normalizeRoleCode(String role) {
        if (role == null || role.isBlank()) {
            return "DRIVER";
        }
        return switch (role.trim().toUpperCase(Locale.ROOT)) {
            case "LIVREUR", "CHAUFFEUR", "DRIVER" -> "DRIVER";
            case "MARKETEUR", "MARKETER" -> "MARKETER";
            case "TRANSPORTEUR", "TRANSPORTER" -> "TRANSPORTER";
            default -> role.trim();
        };
    }

    @Transactional
    public PersonResponse createPersonWithAuth(CreatePersonWithAuthRequest request, String createdBy) {
        PersonResponse personResponse = createPerson(request, createdBy);

        // Assign role if specified or default to DRIVER.
        //
        // The value written to user_role_assignments.roleId MUST be the roles.id
        // UUID, never the code: PermissionService.resolvePermissions looks the
        // assignment up with rolePermissionRepo.findPermissionCodesByRoleId(),
        // which joins on the id. Writing "DRIVER" there produced a user that
        // authenticated successfully but whose JWT carried zero permissions,
        // because nothing resolves a role from its code.
        String requestedRole = request.getRoleName() != null && !request.getRoleName().isBlank()
                ? request.getRoleName()
                : (request.getJobCode() != null ? request.getJobCode() : "DRIVER");
        final String roleCode = normalizeRoleCode(requestedRole);

        try {
            String roleId = roleRepository.findByCode(roleCode)
                    .map(Role::getId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown role code: " + roleCode));

            UserRoleAssignment assignment = new UserRoleAssignment();
            assignment.setPersonId(personResponse.getPersonId());
            assignment.setRoleId(roleId);
            assignment.setOrganizationId(request.getOrganizationId() != null ? request.getOrganizationId() : request.getOrgId());
            assignment.setSiteId(request.getPrimarySiteId());
            assignment.setPrimary(true);
            assignment.setActive(true);
            assignment.setCreatedBy(createdBy != null ? createdBy : "SYSTEM");
            assignment.setCreatedAt(Instant.now());
            roleAssignmentRepository.save(assignment);
            log.info("Assigned role {} ({}) to person {}", roleCode, roleId, personResponse.getPersonId());
        } catch (Exception e) {
            // Loud, because a person without a resolvable role is a user who can
            // log in and then do absolutely nothing — a silent warning here is
            // exactly how that ships unnoticed.
            log.error("Could not assign role {} to person {}: {}",
                    roleCode, personResponse.getPersonId(), e.getMessage(), e);
        }

        // Provision authentication credentials in auth-service
        try {
            Map<String, String> authRequest = new HashMap<>();
            authRequest.put("personId", personResponse.getPersonId());
            authRequest.put("username", request.getUsername());
            authRequest.put("password", request.getPassword());
            authRequest.put("email", request.getEmail());
            authRequest.put("organizationId", request.getOrganizationId());
            authRequest.put("orgId", request.getOrgId() != null ? request.getOrgId() : request.getOrganizationId());

            authClient.register(authRequest);
            log.info("Auth credentials provisioned successfully for person: {}", personResponse.getPersonId());
        } catch (Exception e) {
            log.error("Failed to provision auth credentials for person: {}. Error: {}",
                    personResponse.getPersonId(), e.getMessage());
            throw new RuntimeException("Person created but authentication account provisioning failed: " + e.getMessage(), e);
        }

        return getPerson(personResponse.getId());
    }

    @Transactional
    public PersonResponse updatePerson(String id, UpdatePersonRequest request, String changedBy) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person not found"));

        if (request.getFirstName() != null) person.setFirstName(request.getFirstName());
        if (request.getLastName() != null) person.setLastName(request.getLastName());
        if (person.getFirstName() != null && person.getLastName() != null) {
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
        // Staff transfer — re-home the person to their real org/site.
        if (request.getOrganizationId() != null) person.setOrganizationId(request.getOrganizationId());
        if (request.getOrgId() != null) {
            person.setOrgId(request.getOrgId());
        } else if (request.getOrganizationId() != null) {
            person.setOrgId(request.getOrganizationId());
        }
        if (request.getPrimarySiteId() != null) person.setPrimarySiteId(request.getPrimarySiteId());
        if (request.getSiteId() != null) person.setSiteId(request.getSiteId());
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

    public PersonResponse getPersonByPersonId(String personId) {
        Person person = personRepository.findByPersonId(personId)
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

        return PageResponse.of(list, page.getTotalElements(), page.getNumber(), page.getSize());
    }

    @Transactional
    public PersonResponse updatePersonStatus(String id, UpdateStatusRequest request, String changedBy) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person not found"));
        // Behaviour preserved verbatim: the free-text reason was already being written
        // into status_description, which the Maximo pattern reserves for the label.
        // Two consequences carried over unchanged — a null reason violates the column's
        // NOT NULL, and the "label" reads as prose. Tracked for the Site/Person
        // lifecycle work; not fixed here because this pass is mechanical.
        person.updateStatus(request.getNewStatus(), request.getReason());
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
        response.setStatus(person.getStatus());
        response.setStatusDescription(person.getStatusDescription());
        response.setStatusDate(person.getStatusDate());

        if (person.getPhones() != null) {
            response.setPhones(person.getPhones().stream().map(p -> {
                PhoneDto dto = new PhoneDto();
                dto.setPhoneId(p.getPhoneId());
                dto.setPhoneNum(p.getPhoneNum());
                dto.setType(p.getType());
                dto.setTypeDescription(p.getTypeDescription());
                dto.setPrimary(p.isPrimary());
                return dto;
            }).collect(Collectors.toList()));
        }

        if (person.getEmails() != null) {
            response.setEmails(person.getEmails().stream().map(e -> {
                EmailDto dto = new EmailDto();
                dto.setEmailId(e.getEmailId());
                dto.setEmailAddress(e.getEmailAddress());
                dto.setType(e.getType());
                dto.setTypeDescription(e.getTypeDescription());
                dto.setPrimary(e.isPrimary());
                return dto;
            }).collect(Collectors.toList()));
        }

        List<UserRoleAssignment> roleAssignments = roleAssignmentRepository.findActiveByPersonId(person.getPersonId());
        for (UserRoleAssignment assignment : roleAssignments) {
            roleRepository.findById(assignment.getRoleId())
                    .ifPresent(role -> assignment.setRoleCode(role.getCode()));
        }
        response.setRoles(roleAssignments);
        response.setSites(siteAssignmentRepository.findByPersonIdAndIsActiveTrue(person.getPersonId()));

        response.setModificationsRef("/api/v1/persons/" + person.getId() + "/modifications");
        response.setRecentModifications(new ArrayList<>());

        return response;
    }

    /**
     * Génère un identifiant personne unique.
     *
     * <p>Ne doit surtout pas dépendre d'un compteur en mémoire : celui-ci
     * repartait de sa valeur initiale à chaque redémarrage du pod, si bien que
     * la première personne créée après un redémarrage réattribuait un
     * personId déjà pris et échouait sur l'index unique
     * {@code idx_person_personid} (HTTP 409). Un UUID tronqué garantit
     * l'unicité sans aucun état partagé, quel que soit le nombre de replicas.</p>
     */
    private String generatePersonId() {
        return "P-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 10).toUpperCase(Locale.ROOT);
    }
}
