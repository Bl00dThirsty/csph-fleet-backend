package com.gpl.organization.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.common.dto.PageResponse;
import com.gpl.organization.dto.AnomalyResponse;
import com.gpl.organization.dto.AnomalySummaryResponse;
import com.gpl.organization.service.AnomalyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.gpl.common.security.RequiresPermission;

@RestController
@RequestMapping("/api/v1/anomalies")
@RequiredArgsConstructor
public class AnomalyController {

    private final AnomalyService anomalyService;

    @RequiresPermission("SITE_VIEW")
    @GetMapping
    public PageResponse<AnomalySummaryResponse> listAnomalies(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String siteId,
            Pageable pageable) {
        return anomalyService.listAnomalies(status, severity, category, siteId, pageable);
    }

    @RequiresPermission("SITE_VIEW")
    @GetMapping("/{id}")
    public ApiResponse<AnomalyResponse> getAnomaly(@PathVariable String id) {
        return ApiResponse.success(anomalyService.getAnomaly(id));
    }
}
