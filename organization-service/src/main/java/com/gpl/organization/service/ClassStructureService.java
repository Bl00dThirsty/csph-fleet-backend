package com.gpl.organization.service;

import com.gpl.organization.dto.ClassStructureResponse;
import com.gpl.organization.model.ClassStructure;
import com.gpl.organization.repository.ClassStructureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassStructureService {
    private final ClassStructureRepository classStructureRepository;

    public List<ClassStructureResponse> getAll() {
        return classStructureRepository.findAll().stream().map(this::map).collect(Collectors.toList());
    }

    private ClassStructureResponse map(ClassStructure c) {
        ClassStructureResponse r = new ClassStructureResponse();
        r.setId(c.getId());
        r.setClassificationId(c.getClassificationId());
        r.setDescription(c.getDescription());
        r.setHierarchyPath(c.getHierarchyPath());
        r.setParentClassStructureId(c.getParentClassStructureId());
        r.setObjectName(c.getObjectName());
        r.setSortOrder(c.getSortOrder());
        r.setShow(c.isShow());
        r.setUseClassInDesc(c.isUseClassInDesc());
        r.setTopLevel(c.isTopLevel());
        r.setOrgId(c.getOrgId());
        r.setSiteId(c.getSiteId());
        return r;
    }
}
