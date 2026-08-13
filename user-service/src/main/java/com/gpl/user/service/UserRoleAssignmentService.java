package com.gpl.user.service;

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
