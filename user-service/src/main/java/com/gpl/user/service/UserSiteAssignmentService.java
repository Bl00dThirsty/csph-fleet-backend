package com.gpl.user.service;

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
