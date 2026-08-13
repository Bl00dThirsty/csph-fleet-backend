package com.gpl.cylinder.repository;

import com.gpl.cylinder.model.Cylinder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CylinderRepository extends JpaRepository<Cylinder, String> {
    Optional<Cylinder> findBySerialNumber(String serialNumber);
}
