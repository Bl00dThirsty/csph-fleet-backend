package com.gpl.cylinder.service.impl;

import com.gpl.common.dto.PageResponse;
import com.gpl.common.exception.ResourceNotFoundException;
import com.gpl.cylinder.dto.CreateScanEventRequest;
import com.gpl.cylinder.dto.ScanEventResponse;
import com.gpl.cylinder.model.ScanEvent;
import com.gpl.cylinder.repository.ScanEventRepository;
import com.gpl.cylinder.service.ScanEventService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ScanEventServiceImpl implements ScanEventService {

    private final ScanEventRepository scanEventRepository;

    @Override
    public ScanEventResponse createScanEvent(CreateScanEventRequest request, String createdBy) {
        ScanEvent event = new ScanEvent();
        event.setCheckpointId(request.getCheckpointId());
        event.setLivreurPersonId(request.getLivreurPersonId());
        event.setRfidTagId(request.getRfidTagId());
        event.setDirection(request.getDirection());
        event.setLatitude(request.getLatitude());
        event.setLongitude(request.getLongitude());
        event.setTimestamp(request.getTimestamp() != null ? request.getTimestamp() : Instant.now());
        event.setMeterReading(request.getMeterReading());
        event.setPhotoUrl(request.getPhotoUrl());
        event.setPdaSyncId(request.getPdaSyncId());
        event.setConflictStatus(request.getConflictStatus());
        event.setCreatedBy(createdBy != null ? createdBy : "SYSTEM");
        event.setChangeby(createdBy != null ? createdBy : "SYSTEM");

        ScanEvent saved = scanEventRepository.save(event);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ScanEventResponse getScanEvent(String id) {
        ScanEvent event = scanEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ScanEvent", id));
        return mapToResponse(event);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ScanEventResponse> listScanEvents(
            String checkpointId,
            String livreurPersonId,
            String rfidTagId,
            String direction,
            Pageable pageable) {

        Specification<ScanEvent> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (checkpointId != null && !checkpointId.isBlank()) {
                predicates.add(cb.equal(root.get("checkpointId"), checkpointId));
            }
            if (livreurPersonId != null && !livreurPersonId.isBlank()) {
                predicates.add(cb.equal(root.get("livreurPersonId"), livreurPersonId));
            }
            if (rfidTagId != null && !rfidTagId.isBlank()) {
                predicates.add(cb.equal(root.get("rfidTagId"), rfidTagId));
            }
            if (direction != null && !direction.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("direction")), direction.toLowerCase()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ScanEventResponse> page = scanEventRepository.findAll(spec, pageable).map(this::mapToResponse);
        return PageResponse.of(page);
    }

    @Override
    public ScanEventResponse resolveConflict(String id, com.gpl.cylinder.dto.ResolveScanConflictRequest request, String resolvedBy) {
        ScanEvent event = scanEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ScanEvent", id));

        event.setConflictStatus("RESOLVED");
        String author = (resolvedBy != null && !resolvedBy.isBlank()) ? resolvedBy : "SYSTEM";
        event.setChangeby(author);

        ScanEvent updated = scanEventRepository.save(event);
        return mapToResponse(updated);
    }

    @Override
    public void deleteScanEvent(String id) {
        ScanEvent event = scanEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ScanEvent", id));
        scanEventRepository.delete(event);
    }

    private ScanEventResponse mapToResponse(ScanEvent s) {
        return ScanEventResponse.builder()
                .id(s.getId())
                .rowStamp(s.getRowStamp())
                .checkpointId(s.getCheckpointId())
                .livreurPersonId(s.getLivreurPersonId())
                .rfidTagId(s.getRfidTagId())
                .direction(s.getDirection())
                .latitude(s.getLatitude())
                .longitude(s.getLongitude())
                .timestamp(s.getTimestamp())
                .meterReading(s.getMeterReading())
                .photoUrl(s.getPhotoUrl())
                .pdaSyncId(s.getPdaSyncId())
                .conflictStatus(s.getConflictStatus())
                .createdAt(s.getCreatedAt())
                .createdBy(s.getCreatedBy())
                .changedate(s.getChangedate())
                .changeby(s.getChangeby())
                .build();
    }
}
