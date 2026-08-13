package com.gpl.user.config;

import com.gpl.common.enums.EntityStatus;
import com.gpl.user.model.Person;
import com.gpl.user.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * UserDataInitializer
 *
 * @author GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since 10.08.2026
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "test", "local", "default"})
@Order(1)
public class UserDataInitializer implements CommandLineRunner {

    private final PersonRepository personRepository;

    /*
     * run
     */
    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initializing test data for users/persons...");

        if (personRepository.count() > 0) {
            log.info("Persons already exist, skipping initialization");
            return;
        }

        // Admin CSPH
        createPerson("admin.cspHq", "Admin", "CSPH", "admin.cspHq@cspHq.cm",
                "CSPH", "CSPH", "SITE-CSPH-HQ", "Administrateur Système", "ADMIN-001",
                "+237 6 XX XX XX XX", "Yaoundé", "FR", null, 0);

        // Opérateur Dépôt Douala
        createPerson("operateur.dla", "Jean", "Kouam", "jean.kouam@depot-dla.cm",
                "DEP-DLA", "DEP-DLA", "SITE-DEP-DLA-PRINCIPAL", "Opérateur Dépôt", "OPE-DEP-001",
                "+237 6 XX XX XX XX", "Douala", "FR", null, 1);

        // Opérateur Centre Emplisseur Douala
        createPerson("emplisseur.dla", "Marie", "Ngono", "marie.ngono@depot-dla.cm",
                "DEP-DLA", "DEP-DLA", "SITE-DEP-DLA-FIL", "Opérateur Emplissage", "OPE-FIL-001",
                "+237 6 XX XX XX XX", "Douala", "FR", "operateur.dla", 1);

        // Opérateur Dépôt Yaoundé
        createPerson("operateur.yde", "Paul", "Mvondo", "paul.mvondo@depot-yde.cm",
                "DEP-YDE", "DEP-YDE", "SITE-DEP-YDE-PRINCIPAL", "Opérateur Dépôt", "OPE-DEP-002",
                "+237 6 XX XX XX XX", "Yaoundé", "FR", null, 1);

        // Gestionnaire GPL
        createPerson("gest.gpl", "Alice", "Fouda", "alice.fouda@gpl.cm",
                "MKT-GPL", "MKT-GPL", "SITE-MKT-GPL-ENTREPOT", "Gestionnaire Stocks", "GES-STK-001",
                "+237 6 XX XX XX XX", "Douala", "FR", null, 2);

        // Chauffeur ABC Transport
        createPerson("chauffeur.abc1", "Pierre", "Essomba", "pierre.essomba@abctransport.cm",
                "TRP-ABC", "TRP-ABC", "SITE-TRP-ABC-DEPOT", "Chauffeur Livreur", "CHF-001",
                "+237 6 XX XX XX XX", "Douala", "FR", null, 3);

        createPerson("chauffeur.abc2", "Samuel", "Ondoa", "samuel.ondoa@abctransport.cm",
                "TRP-ABC", "TRP-ABC", "SITE-TRP-ABC-DEPOT", "Chauffeur Livreur", "CHF-002",
                "+237 6 XX XX XX XX", "Douala", "FR", null, 3);

        // Responsable Industries Client
        createPerson("resp.industries", "Christelle", "Moukoko", "christelle.moukoko@industries.cm",
                "CLT-IND", "CLT-IND", "SITE-CLT-IND-RECEPTION", "Responsable Approvisionnement", "RES-APP-001",
                "+237 6 XX XX XX XX", "Douala", "FR", null, 2);

        // Superviseur CSPH
        createPerson("superviseur.cspHq", "Robert", "Atangana", "robert.atangana@cspHq.cm",
                "CSPH", "CSPH", "SITE-CSPH-HQ", "Superviseur Régional", "SUP-REG-001",
                "+237 6 XX XX XX XX", "Yaoundé", "FR", "admin.cspHq", 1);

        // Super Admin CSPH
        createPerson("superadmin.cspHq", "Emmanuel", "Mbarga", "emmanuel.mbarga@cspHq.cm",
                "CSPH", "CSPH", "SITE-CSPH-HQ", "Directeur Général Adjoint", "DGA-001",
                "+237 6 XX XX XX XX", "Yaoundé", "FR", null, 0);

        // Intégrateur Technique CSPH
        createPerson("integrateur.cspHq", "Fabrice", "Ndjock", "fabrice.ndjock@cspHq.cm",
                "CSPH", "CSPH", "SITE-CSPH-HQ", "Intégrateur Système", "INT-SYS-001",
                "+237 6 XX XX XX XX", "Yaoundé", "FR", "admin.cspHq", 1);

        // Responsable Transport TRP-ABC
        createPerson("resp.abc", "Jacques", "Tabi", "jacques.tabi@abctransport.cm",
                "TRP-ABC", "TRP-ABC", "SITE-TRP-ABC-DEPOT", "Responsable Opérations Transport", "RES-TRP-001",
                "+237 6 XX XX XX XX", "Douala", "FR", null, 2);

        // Agent Terrain Marketeur
        createPerson("agent.gpl", "Diane", "Ekotto", "diane.ekotto@gpl.cm",
                "MKT-GPL", "MKT-GPL", "SITE-MKT-GPL-ENTREPOT", "Agent Terrain Distribution", "AGT-DST-001",
                "+237 6 XX XX XX XX", "Douala", "FR", "gest.gpl", 1);

        log.info("Created {} persons", personRepository.count());
        log.info("User test data initialization complete!");
    }

    /*
     * createPerson
     */
    private void createPerson(String personId, String firstName, String lastName, String email,
                              String organizationId, String orgId, String primarySiteId,
                              String title, String jobCode,
                              String primaryPhone, String city, String language,
                              String supervisorId, int deviceClass) {
        
        if (personRepository.existsByPersonId(personId)) {
            log.debug("Person {} already exists, skipping", personId);
            return;
        }
        
        Person person = new Person();
        person.setPersonId(personId);
        person.setPersonUid(System.currentTimeMillis() % 100000);
        person.setOrganizationId(organizationId);
        person.setOrgId(orgId);
        person.setPrimarySiteId(primarySiteId);
        person.setSiteId(primarySiteId);
        person.setLocationOrg(orgId);
        person.setLocationSite(primarySiteId);
        person.setEmail(email);
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setDisplayName(lastName.toUpperCase() + " " + firstName);
        person.setTitle(title);
        person.setJobCode(jobCode);
        person.setJobCodeDescription(getJobDescription(jobCode));
        person.setPrimaryPhone(primaryPhone);
        person.setAddressLine1("");
        person.setCity(city);
        person.setLanguage(language != null ? language : "FR");
        person.setSupervisorId(supervisorId);
        person.setDeviceClass(deviceClass);
        person.setDeviceClassDescription(getDeviceClassDescription(deviceClass));
        
        person.setStatus(EntityStatus.ACTIVE.getCode());
        person.setStatusDescription(EntityStatus.ACTIVE.getDescription());
        person.setStatusDate(Instant.now());
        person.setCreatedBy("SYSTEM_INIT");
        person.setActive(true);
        person.setLocked(false);
        person.setCertified(false);
        person.setAcceptingWfMail(true);
        person.setLocToServReq(false);
        person.setStatusIface(false);
        person.setWfMailElection("PROCESS");
        person.setTransEmailElection("NEVER");

        personRepository.save(person);
    }

    /*
     * generatePersonId
     */
    private String generatePersonId() {
        long seq = System.currentTimeMillis() % 100000;
        char checkLetter = (char) ('A' + (seq % 26));
        return seq + "-" + checkLetter;
    }

    /*
     * getJobDescription
     */
    private String getJobDescription(String jobCode) {
        return switch (jobCode) {
            case "ADMIN-001" -> "Administrateur Système";
            case "OPE-DEP-001", "OPE-DEP-002" -> "Opérateur Dépôt";
            case "OPE-FIL-001" -> "Opérateur Emplissage";
            case "GES-STK-001" -> "Gestionnaire Stocks";
            case "CHF-001", "CHF-002" -> "Chauffeur Livreur";
            case "RES-APP-001" -> "Responsable Approvisionnement";
            case "SUP-REG-001" -> "Superviseur Régional";
            case "DGA-001" -> "Directeur Général Adjoint";
            case "INT-SYS-001" -> "Intégrateur Système";
            case "RES-TRP-001" -> "Responsable Opérations Transport";
            case "AGT-DST-001" -> "Agent Terrain Distribution";
            default -> jobCode;
        };
    }

    /*
     * getDeviceClassDescription
     */
    private String getDeviceClassDescription(int deviceClass) {
        return switch (deviceClass) {
            case 0 -> "Administrateur / Bureau";
            case 1 -> "Opérateur Terrain";
            case 2 -> "Gestionnaire / Superviseur";
            case 3 -> "Chauffeur / Livreur";
            default -> "Non défini";
        };
    }
}