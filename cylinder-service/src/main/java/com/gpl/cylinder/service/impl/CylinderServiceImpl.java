package com.gpl.cylinder.service.impl;

import com.gpl.common.dto.PageResponse;
import com.gpl.common.exception.DuplicateResourceException;
import com.gpl.common.exception.ResourceNotFoundException;
import com.gpl.cylinder.dto.CreateCylinderRequest;
import com.gpl.cylinder.dto.UpdateCylinderRequest;
import com.gpl.cylinder.dto.CylinderResponse;
import com.gpl.cylinder.model.Cylinder;
import com.gpl.cylinder.repository.CylinderRepository;
import com.gpl.cylinder.service.CylinderService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CylinderServiceImpl implements CylinderService {

    private final CylinderRepository cylinderRepository;

    @Override
    public CylinderResponse createCylinder(CreateCylinderRequest request, String createdBy) {
        if (request.getSerialNumber() != null && cylinderRepository.existsBySerialNumber(request.getSerialNumber())) {
            throw new DuplicateResourceException("Une bouteille avec le numéro de série " + request.getSerialNumber() + " existe déjà.");
        }

        Cylinder cylinder = new Cylinder();
        cylinder.setSerialNumber(request.getSerialNumber());
        cylinder.setBarcode(request.getBarcode());
        cylinder.setCylinderTypeId(request.getCylinderTypeId());
        cylinder.setOwnerOrganizationId(request.getOwnerOrganizationId());
        cylinder.setCurrentHolderOrganizationId(request.getCurrentHolderOrganizationId());
        cylinder.setCurrentSiteId(request.getCurrentSiteId());
        cylinder.setFillStatus(request.getFillStatus());
        cylinder.setBrand(request.getBrand());
        cylinder.setCapacityKg(request.getCapacityKg());
        cylinder.setCreatedBy(createdBy != null ? createdBy : "SYSTEM");
        cylinder.setChangeby(createdBy != null ? createdBy : "SYSTEM");

        if (request.getStatus() != null || request.getStatusDescription() != null) {
            cylinder.updateStatus(
                    request.getStatus() != null ? request.getStatus() : cylinder.getStatus(),
                    request.getStatusDescription() != null
                            ? request.getStatusDescription() : cylinder.getStatusDescription());
        }

        Cylinder saved = cylinderRepository.save(cylinder);
        return mapToResponse(saved);
    }

    @Override
    public CylinderResponse updateCylinder(String id, UpdateCylinderRequest request, String changedBy) {
        Cylinder cylinder = cylinderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cylinder", id));

        if (request.getSerialNumber() != null && !request.getSerialNumber().equals(cylinder.getSerialNumber())) {
            if (cylinderRepository.existsBySerialNumber(request.getSerialNumber())) {
                throw new DuplicateResourceException("Une bouteille avec le numéro de série " + request.getSerialNumber() + " existe déjà.");
            }
            cylinder.setSerialNumber(request.getSerialNumber());
        }

        if (request.getBarcode() != null) cylinder.setBarcode(request.getBarcode());
        if (request.getCylinderTypeId() != null) cylinder.setCylinderTypeId(request.getCylinderTypeId());
        if (request.getOwnerOrganizationId() != null) cylinder.setOwnerOrganizationId(request.getOwnerOrganizationId());
        if (request.getCurrentHolderOrganizationId() != null) cylinder.setCurrentHolderOrganizationId(request.getCurrentHolderOrganizationId());
        if (request.getCurrentSiteId() != null) cylinder.setCurrentSiteId(request.getCurrentSiteId());
        if (request.getFillStatus() != null) cylinder.setFillStatus(request.getFillStatus());
        if (request.getBrand() != null) cylinder.setBrand(request.getBrand());
        if (request.getCapacityKg() != null) cylinder.setCapacityKg(request.getCapacityKg());

        if (request.getStatus() != null || request.getStatusDescription() != null) {
            cylinder.updateStatus(
                    request.getStatus() != null ? request.getStatus() : cylinder.getStatus(),
                    request.getStatusDescription() != null
                            ? request.getStatusDescription() : cylinder.getStatusDescription());
        }

        cylinder.setChangeby(changedBy != null ? changedBy : "SYSTEM");

        Cylinder updated = cylinderRepository.save(cylinder);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public CylinderResponse getCylinder(String id) {
        Cylinder cylinder = cylinderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cylinder", id));
        return mapToResponse(cylinder);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CylinderResponse> listCylinders(
            String ownerOrganizationId,
            String currentSiteId,
            String brand,
            Double capacityKg,
            String status,
            Pageable pageable) {

        Specification<Cylinder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (ownerOrganizationId != null && !ownerOrganizationId.isBlank()) {
                predicates.add(cb.equal(root.get("ownerOrganizationId"), ownerOrganizationId));
            }
            if (currentSiteId != null && !currentSiteId.isBlank()) {
                predicates.add(cb.equal(root.get("currentSiteId"), currentSiteId));
            }
            if (brand != null && !brand.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("brand")), brand.toLowerCase()));
            }
            if (capacityKg != null) {
                predicates.add(cb.equal(root.get("capacityKg"), capacityKg));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<CylinderResponse> page = cylinderRepository.findAll(spec, pageable).map(this::mapToResponse);
        return PageResponse.of(page);
    }

    @Override
    public CylinderResponse transferCylinder(String id, com.gpl.cylinder.dto.TransferCylinderRequest request, String transferredBy) {
        Cylinder cylinder = cylinderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cylinder", id));

        cylinder.setCurrentSiteId(request.getTargetSiteId());
        if (request.getTargetHolderOrganizationId() != null && !request.getTargetHolderOrganizationId().isBlank()) {
            cylinder.setCurrentHolderOrganizationId(request.getTargetHolderOrganizationId());
        }
        if (request.getFillStatus() != null && !request.getFillStatus().isBlank()) {
            cylinder.setFillStatus(request.getFillStatus());
        }

        String author = (transferredBy != null && !transferredBy.isBlank()) ? transferredBy : "SYSTEM";
        cylinder.setChangeby(author);
        String auditMsg = "Transféré vers le site " + request.getTargetSiteId();
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            auditMsg += " (" + request.getNotes() + ")";
        }
        // Behaviour preserved: the transfer never changed the status code, it only
        // re-stamped label and date. Passing the current code back in keeps the
        // triple consistent without inventing a transition. The prose-as-label
        // misuse is inherited, not introduced — tracked with the Site/Person one.
        cylinder.updateStatus(cylinder.getStatus(), auditMsg);

        Cylinder updated = cylinderRepository.save(cylinder);
        return mapToResponse(updated);
    }

    @Override
    public void deleteCylinder(String id) {
        Cylinder cylinder = cylinderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cylinder", id));
        cylinderRepository.delete(cylinder);
    }

    private CylinderResponse mapToResponse(Cylinder c) {
        return CylinderResponse.builder()
                .id(c.getId())
                .rowStamp(c.getRowStamp())
                .serialNumber(c.getSerialNumber())
                .barcode(c.getBarcode())
                .cylinderTypeId(c.getCylinderTypeId())
                .ownerOrganizationId(c.getOwnerOrganizationId())
                .currentHolderOrganizationId(c.getCurrentHolderOrganizationId())
                .currentSiteId(c.getCurrentSiteId())
                .fillStatus(c.getFillStatus())
                .brand(c.getBrand())
                .capacityKg(c.getCapacityKg())
                .status(c.getStatus())
                .statusDescription(c.getStatusDescription())
                .statusDate(c.getStatusDate())
                .createdAt(c.getCreatedAt())
                .createdBy(c.getCreatedBy())
                .changedate(c.getChangedate())
                .changeby(c.getChangeby())
                .build();
    }
}
