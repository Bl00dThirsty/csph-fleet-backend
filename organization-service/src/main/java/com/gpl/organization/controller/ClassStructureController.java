package com.gpl.organization.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.organization.dto.ClassStructureResponse;
import com.gpl.organization.service.ClassStructureService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.gpl.common.security.RequiresPermission;

import java.util.List;

@RestController
@RequestMapping("/api/v1/classifications")
@RequiredArgsConstructor
public class ClassStructureController {
    private final ClassStructureService classStructureService;

    @RequiresPermission("CLASS_VIEW")
    @GetMapping
    public ApiResponse<List<ClassStructureResponse>> getAll() {
        return ApiResponse.success(classStructureService.getAll());
    }
}
