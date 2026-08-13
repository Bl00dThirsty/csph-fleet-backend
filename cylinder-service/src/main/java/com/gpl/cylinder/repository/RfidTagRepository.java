package com.gpl.cylinder.repository;

import com.gpl.cylinder.model.RfidTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RfidTagRepository extends JpaRepository<RfidTag, String>, JpaSpecificationExecutor<RfidTag> {
    Optional<RfidTag> findByTagUid(String tagUid);
    boolean existsByTagUid(String tagUid);
}
