package com.gpl.subsidy.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.subsidy.model.Declaration;
import com.gpl.subsidy.repository.DeclarationRepository;
import com.gpl.common.security.RequiresPermission;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/declarations")
public class DeclarationController {
    private final DeclarationRepository declarationRepository;

    public DeclarationController(DeclarationRepository declarationRepository) {
        this.declarationRepository = declarationRepository;
    }

    @RequiresPermission("DECLARATION_VIEW")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Declaration>>> listDeclarations() {
        return ResponseEntity.ok(ApiResponse.ok(declarationRepository.findAll()));
    }

    @RequiresPermission("DECLARATION_CREATE")
    @PostMapping
    public ResponseEntity<ApiResponse<Declaration>> createDeclaration(@RequestBody Declaration declaration) {
        return ResponseEntity.ok(ApiResponse.ok(declarationRepository.save(declaration)));
    }
}
