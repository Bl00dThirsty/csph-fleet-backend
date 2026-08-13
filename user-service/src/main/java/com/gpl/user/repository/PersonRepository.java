package com.gpl.user.repository;
import com.gpl.user.model.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface PersonRepository extends JpaRepository<Person, String> {
    Optional<Person> findByPersonId(String personId);
    Page<Person> findByOrganizationId(String organizationId, Pageable pageable);
    List<Person> findBySupervisorId(String supervisorId);
    List<Person> findByOrgIdAndSiteId(String orgId, String siteId);
    List<Person> findByJobCode(String jobCode);
    Page<Person> findByDisplayNameContainingIgnoreCase(String search, Pageable pageable);
    Page<Person> findByIsActiveTrue(Pageable pageable);
    boolean existsByPersonId(String personId);
}
