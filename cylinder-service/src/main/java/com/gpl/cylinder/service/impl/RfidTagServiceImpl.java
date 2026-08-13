package com.gpl.cylinder.service.impl;

import com.gpl.common.dto.PageResponse;
import com.gpl.common.exception.DuplicateResourceException;
import com.gpl.common.exception.ResourceNotFoundException;
import com.gpl.cylinder.dto.CreateRfidTagRequest;
import com.gpl.cylinder.dto.UpdateRfidTagRequest;
import com.gpl.cylinder.dto.RfidTagResponse;
import com.gpl.cylinder.model.RfidTag;
import com.gpl.cylinder.repository.RfidTagRepository;
import com.gpl.cylinder.service.RfidTagService;
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
public class RfidTagServiceImpl implements RfidTagService {

    private final RfidTagRepository rfidTagRepository;

    @Override
    public RfidTagResponse createRfidTag(CreateRfidTagRequest request, String createdBy) {
        if (request.getTagUid() != null && rfidTagRepository.existsByTagUid(request.getTagUid())) {
            throw new DuplicateResourceException("Un tag RFID avec l'UID " + request.getTagUid() + " existe déjà.");
        }

        RfidTag tag = new RfidTag();
        tag.setTagUid(request.getTagUid());
        tag.setBottleSerial(request.getBottleSerial());
        tag.setCurrentSiteId(request.getCurrentSiteId());
        tag.setCurrentClientSiteId(request.getCurrentClientSiteId());
        tag.setCreatedBy(createdBy != null ? createdBy : "SYSTEM");
        tag.setChangeby(createdBy != null ? createdBy : "SYSTEM");

        if (request.getStatus() != null) {
            tag.setStatus(request.getStatus());
        }
        if (request.getStatusDescription() != null) {
            tag.setStatusDescription(request.getStatusDescription());
        }

        RfidTag saved = rfidTagRepository.save(tag);
        return mapToResponse(saved);
    }

    @Override
    public RfidTagResponse updateRfidTag(String id, UpdateRfidTagRequest request, String changedBy) {
        RfidTag tag = rfidTagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RfidTag", id));

        if (request.getTagUid() != null && !request.getTagUid().equals(tag.getTagUid())) {
            if (rfidTagRepository.existsByTagUid(request.getTagUid())) {
                throw new DuplicateResourceException("Un tag RFID avec l'UID " + request.getTagUid() + " existe déjà.");
            }
            tag.setTagUid(request.getTagUid());
        }

        if (request.getBottleSerial() != null) tag.setBottleSerial(request.getBottleSerial());
        if (request.getCurrentSiteId() != null) tag.setCurrentSiteId(request.getCurrentSiteId());
        if (request.getCurrentClientSiteId() != null) tag.setCurrentClientSiteId(request.getCurrentClientSiteId());

        if (request.getStatus() != null) {
            tag.setStatus(request.getStatus());
        }
        if (request.getStatusDescription() != null) {
            tag.setStatusDescription(request.getStatusDescription());
        }

        tag.setChangeby(changedBy != null ? changedBy : "SYSTEM");

        RfidTag updated = rfidTagRepository.save(tag);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public RfidTagResponse getRfidTag(String id) {
        RfidTag tag = rfidTagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RfidTag", id));
        return mapToResponse(tag);
    }

    @Override
    @Transactional(readOnly = true)
    public RfidTagResponse getByTagUid(String tagUid) {
        RfidTag tag = rfidTagRepository.findByTagUid(tagUid)
                .orElseThrow(() -> new ResourceNotFoundException("RfidTag", "tagUid", tagUid));
        return mapToResponse(tag);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RfidTagResponse> listRfidTags(
            String tagUid,
            String bottleSerial,
            String currentSiteId,
            String status,
            Pageable pageable) {

        Specification<RfidTag> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (tagUid != null && !tagUid.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("tagUid")), tagUid.toLowerCase()));
            }
            if (bottleSerial != null && !bottleSerial.isBlank()) {
                predicates.add(cb.equal(root.get("bottleSerial"), bottleSerial));
            }
            if (currentSiteId != null && !currentSiteId.isBlank()) {
                predicates.add(cb.equal(root.get("currentSiteId"), currentSiteId));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<RfidTagResponse> page = rfidTagRepository.findAll(spec, pageable).map(this::mapToResponse);
        return PageResponse.of(page);
    }

    @Override
    public void deleteRfidTag(String id) {
        RfidTag tag = rfidTagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RfidTag", id));
        rfidTagRepository.delete(tag);
    }

    private RfidTagResponse mapToResponse(RfidTag t) {
        return RfidTagResponse.builder()
                .id(t.getId())
                .rowStamp(t.getRowStamp())
                .tagUid(t.getTagUid())
                .bottleSerial(t.getBottleSerial())
                .currentSiteId(t.getCurrentSiteId())
                .currentClientSiteId(t.getCurrentClientSiteId())
                .status(t.getStatus())
                .statusDescription(t.getStatusDescription())
                .statusDate(t.getStatusDate())
                .createdAt(t.getCreatedAt())
                .createdBy(t.getCreatedBy())
                .changedate(t.getChangedate())
                .changeby(t.getChangeby())
                .build();
    }
}
