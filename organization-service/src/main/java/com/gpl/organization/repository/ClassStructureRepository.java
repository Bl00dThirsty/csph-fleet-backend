package com.gpl.organization.repository;

import com.gpl.organization.model.ClassStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassStructureRepository extends JpaRepository<ClassStructure, String> {
    List<ClassStructure> findByObjectName(String objectName);
    List<ClassStructure> findByParentClassStructureId(String parentClassStructureId);
    List<ClassStructure> findByHierarchyPathStartingWith(String hierarchyPathPrefix);
    List<ClassStructure> findByOrgIdAndSiteId(String orgId, String siteId);
}
