package com.gpl.tour.repository;

import com.gpl.tour.model.Tour;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Repository JPA pour l'acces aux donnees des tournees.
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   04.08.2026
 */
public interface TourRepository extends JpaRepository<Tour, String> {
    List<Tour> findByMarketerOrganizationId(String orgId);
    List<Tour> findByTransporterOrganizationId(String orgId);
}
