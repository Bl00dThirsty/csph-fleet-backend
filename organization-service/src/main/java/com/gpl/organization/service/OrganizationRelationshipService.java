package com.gpl.organization.service;

import com.gpl.organization.dto.CreateRelationshipRequest;
import com.gpl.organization.dto.RelationshipResponse;
import com.gpl.organization.model.Organization;
import com.gpl.organization.model.OrganizationRelationship;
import com.gpl.organization.repository.OrganizationRelationshipRepository;
import com.gpl.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationRelationshipService {
    private final OrganizationRelationshipRepository relationshipRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional
    public RelationshipResponse createRelationship(CreateRelationshipRequest request, String createdBy) {
        Organization source = organizationRepository.findById(request.getSourceOrganizationId())
                .orElseThrow(() -> new RuntimeException("Source org not found"));
        Organization target = organizationRepository.findById(request.getTargetOrganizationId())
                .orElseThrow(() -> new RuntimeException("Target org not found"));

        if (source.getId().equals(target.getId())) {
            throw new RuntimeException("Cannot create self-relationship");
        }

        OrganizationRelationship rel = OrganizationRelationship.builder()
                .sourceOrganizationId(source.getId())
                .targetOrganizationId(target.getId())
                .type(request.getType())
                .contractReference(request.getContractReference())
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .isExclusive(request.isExclusive())
                .description(request.getDescription())
                .build();
        
        rel.setCreatedBy(createdBy);
        return buildResponse(relationshipRepository.save(rel), source.getName(), target.getName());
    }

    private RelationshipResponse buildResponse(OrganizationRelationship rel, String srcName, String tgtName) {
        RelationshipResponse res = new RelationshipResponse();
        res.setId(rel.getId());
        res.setSourceOrganizationId(rel.getSourceOrganizationId());
        res.setTargetOrganizationId(rel.getTargetOrganizationId());
        res.setSourceOrganizationName(srcName);
        res.setTargetOrganizationName(tgtName);
        res.setType(rel.getType());
        res.setTypeDescription(rel.getTypeDescription());
        res.setContractReference(rel.getContractReference());
        res.setValidFrom(rel.getValidFrom());
        res.setValidUntil(rel.getValidUntil());
        res.setActive(rel.isActive());
        res.setExclusive(rel.isExclusive());
        res.setDescription(rel.getDescription());
        res.setStatus(rel.getStatus());
        return res;
    }
}
