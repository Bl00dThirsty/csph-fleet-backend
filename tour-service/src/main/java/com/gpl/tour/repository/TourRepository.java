package com.gpl.tour.repository;

import com.gpl.tour.model.Tour;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * Tours assigned to one driver, paginated.
     *
     * <p>Exists because {@code GET /api/v1/tours?driverPersonId=…} binds a bare
     * {@code Pageable} and the unknown query parameter is silently discarded: the
     * PDA asked for its driver's tours and was handed the marketer's whole fleet.
     * A null or blank driver must therefore resolve to {@code findAll}, not to an
     * empty page — see {@code TourServiceImpl.getAll}.</p>
     */
    Page<Tour> findByDriverPersonId(String driverPersonId, Pageable pageable);
}
