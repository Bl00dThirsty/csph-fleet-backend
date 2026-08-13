package com.gpl.tour.service;

import com.gpl.common.dto.PageResponse;
import com.gpl.tour.dto.CreateTransporterContractDto;
import com.gpl.tour.dto.TransporterContractResponseDto;
import com.gpl.tour.dto.UpdateTransporterContractDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TransporterContractService {
    TransporterContractResponseDto createContract(CreateTransporterContractDto dto, String createdBy);
    TransporterContractResponseDto updateContract(String id, UpdateTransporterContractDto dto, String updatedBy);
    TransporterContractResponseDto getContract(String id);
    PageResponse<TransporterContractResponseDto> listContracts(String marketerOrganizationId, String transporterOrganizationId, Pageable pageable);
    List<TransporterContractResponseDto> getContractsByMarketer(String marketerOrganizationId);
    TransporterContractResponseDto terminateContract(String id, String terminatedBy);
    void deleteContract(String id);
}
