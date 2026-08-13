package com.gpl.subsidy.repository;

import com.gpl.subsidy.model.Declaration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeclarationRepository extends JpaRepository<Declaration, String> {
    List<Declaration> findByMarketerOrganizationId(String orgId);
}
