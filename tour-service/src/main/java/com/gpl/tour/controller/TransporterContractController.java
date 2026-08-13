package com.gpl.tour.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.common.dto.PageResponse;
import com.gpl.common.security.RequiresPermission;
import com.gpl.tour.dto.CreateTransporterContractDto;
import com.gpl.tour.dto.TransporterContractResponseDto;
import com.gpl.tour.dto.UpdateTransporterContractDto;
import com.gpl.tour.service.TransporterContractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller pour la gestion des contrats entre marketeurs et transporteurs.
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   13.08.2026
 */
@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class TransporterContractController {

    private final TransporterContractService contractService;

    @RequiresPermission("CONTRACT_VIEW")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TransporterContractResponseDto>>> listContracts(
            @RequestParam(required = false) String marketerOrganizationId,
            @RequestParam(required = false) String transporterOrganizationId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(contractService.listContracts(marketerOrganizationId, transporterOrganizationId, pageable)));
    }

    @RequiresPermission("CONTRACT_VIEW")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransporterContractResponseDto>> getContract(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(contractService.getContract(id)));
    }

    @RequiresPermission("CONTRACT_VIEW")
    @GetMapping("/by-marketer/{marketerOrganizationId}")
    public ResponseEntity<ApiResponse<List<TransporterContractResponseDto>>> getContractsByMarketer(@PathVariable String marketerOrganizationId) {
        return ResponseEntity.ok(ApiResponse.success(contractService.getContractsByMarketer(marketerOrganizationId)));
    }

    @RequiresPermission("CONTRACT_CREATE")
    @PostMapping
    public ResponseEntity<ApiResponse<TransporterContractResponseDto>> createContract(
            @Valid @RequestBody CreateTransporterContractDto dto,
            @RequestHeader(value = "X-User-PersonId", required = false) String createdBy) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(contractService.createContract(dto, createdBy)));
    }

    @RequiresPermission("CONTRACT_UPDATE")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransporterContractResponseDto>> updateContract(
            @PathVariable String id,
            @Valid @RequestBody UpdateTransporterContractDto dto,
            @RequestHeader(value = "X-User-PersonId", required = false) String updatedBy) {
        return ResponseEntity.ok(ApiResponse.success(contractService.updateContract(id, dto, updatedBy)));
    }

    @RequiresPermission("CONTRACT_DELETE")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteContract(@PathVariable String id) {
        contractService.deleteContract(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
