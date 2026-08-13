package com.gpl.subsidy.repository;

import com.gpl.subsidy.model.Declaration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeclarationRepository extends JpaRepository<Declaration, String>, JpaSpecificationExecutor<Declaration> {
    List<Declaration> findByMarketerOrganizationId(String orgId);
    List<Declaration> findByDeclaringOrganizationId(String orgId);
}
