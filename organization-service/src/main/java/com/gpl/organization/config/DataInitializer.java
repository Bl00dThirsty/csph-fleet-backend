package com.gpl.organization.config;

import com.gpl.common.enums.OrganizationTier;
import com.gpl.common.enums.OrganizationType;
import com.gpl.common.enums.SiteType;
import com.gpl.organization.model.Organization;
import com.gpl.organization.model.Site;
import com.gpl.organization.repository.OrganizationRepository;
import com.gpl.organization.repository.SiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "test", "local", "default"})
public class DataInitializer implements CommandLineRunner {

    private final OrganizationRepository organizationRepository;
    private final SiteRepository siteRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initializing test data for organizations and sites...");

        if (organizationRepository.count() > 0) {
            log.info("Organizations already exist, skipping initialization");
            return;
        }

        Organization cspHq = createOrganization("CSPH", "CSPH Headquarters", "Régulateur du secteur GPL", 
                OrganizationType.REGULATOR.getCode(), OrganizationTier.TIER_1_GOVERNANCE.getCode(),
                "cspHq@cspHq.cm", "+237 2 XX XX XX XX", "https://cspHq.cm", true, null);

        Organization depotDouala = createOrganization("DEP-DLA", "Dépôt Douala", "Dépôt principal de Douala",
                OrganizationType.DEPOT.getCode(), OrganizationTier.TIER_2_INFRASTRUCTURE.getCode(),
                "dla@depot.cm", "+237 2 XX XX XX XX", "https://depot-dla.cm", false, cspHq.getId());

        Organization depotYaounde = createOrganization("DEP-YDE", "Dépôt Yaoundé", "Dépôt principal de Yaoundé",
                OrganizationType.DEPOT.getCode(), OrganizationTier.TIER_2_INFRASTRUCTURE.getCode(),
                "yde@depot.cm", "+237 2 XX XX XX XX", "https://depot-yde.cm", false, cspHq.getId());

        Organization marketeurGpl = createOrganization("MKT-GPL", "GPL Cameroun", "Marqueteur principal",
                OrganizationType.MARKETER.getCode(), OrganizationTier.TIER_3_OPERATIONS.getCode(),
                "contact@gpl.cm", "+237 2 XX XX XX XX", "https://gpl.cm", false, cspHq.getId());

        Organization transporteurABC = createOrganization("TRP-ABC", "ABC Transport", "Transporteur agréé",
                OrganizationType.TRANSPORTER.getCode(), OrganizationTier.TIER_3_OPERATIONS.getCode(),
                "ops@abctransport.cm", "+237 2 XX XX XX XX", "https://abctransport.cm", false, cspHq.getId());

        Organization clientIndustrial = createOrganization("CLT-IND", "Industries du Cameroun", "Client industriel",
                OrganizationType.CLIENT.getCode(), OrganizationTier.TIER_4_CONSUMPTION.getCode(),
                "achats@industries.cm", "+237 2 XX XX XX XX", "https://industries.cm", false, marketeurGpl.getId());

        log.info("Created {} organizations", organizationRepository.count());

        createSite("SITE-CSPH-HQ", "CSPH-HQ-001", "Siège CSPH", "Siège social du régulateur",
                cspHq.getId(), cspHq.getCode(), SiteType.OFFICE.getCode(),
                "Bastos, Yaoundé", "Yaoundé", "Centre", 3.8480, 11.5021);

        createSite("SITE-DEP-DLA-PRINCIPAL", "DLA-DEP-001", "Dépôt Principal Douala", "Dépôt de stockage principal Douala",
                depotDouala.getId(), depotDouala.getCode(), SiteType.DEPOT.getCode(),
                "Zone Portuaire, Douala", "Douala", "Littoral", 4.0511, 9.7679);

        createSite("SITE-DEP-DLA-FIL", "DLA-FIL-001", "Centre Emplisseur Douala", "Centre d'emplissage Douala",
                depotDouala.getId(), depotDouala.getCode(), SiteType.FILLING_CENTER.getCode(),
                "Zone Portuaire, Douala", "Douala", "Littoral", 4.0450, 9.7700);

        createSite("SITE-DEP-YDE-PRINCIPAL", "YDE-DEP-001", "Dépôt Principal Yaoundé", "Dépôt de stockage principal Yaoundé",
                depotYaounde.getId(), depotYaounde.getCode(), SiteType.DEPOT.getCode(),
                "Nsam, Yaoundé", "Yaoundé", "Centre", 3.8612, 11.5217);

        createSite("SITE-MKT-GPL-ENTREPOT", "GPL-WHS-001", "Entrepôt GPL Douala", "Entrepôt de distribution GPL",
                marketeurGpl.getId(), marketeurGpl.getCode(), SiteType.WAREHOUSE.getCode(),
                "Bassa, Douala", "Douala", "Littoral", 4.0333, 9.7167);

        createSite("SITE-TRP-ABC-DEPOT", "ABC-DEP-001", "Dépôt ABC Transport", "Dépôt véhicules ABC Transport",
                transporteurABC.getId(), transporteurABC.getCode(), SiteType.DEPOT.getCode(),
                "PK12, Douala", "Douala", "Littoral", 4.0000, 9.8000);

        createSite("SITE-CLT-IND-RECEPTION", "IND-REC-001", "Zone Réception Industries", "Zone de réception bouteilles",
                clientIndustrial.getId(), clientIndustrial.getCode(), SiteType.CLIENT_SITE.getCode(),
                "Zone Industrielle, Douala", "Douala", "Littoral", 4.0167, 9.7000);

        log.info("Created {} sites", siteRepository.count());
        log.info("Test data initialization complete!");
    }

    private Organization createOrganization(String code, String name, String description,
                                            String type, String tier,
                                            String contactEmail, String contactPhone, String website,
                                            boolean isHeadquarters, String parentId) {
        Organization org = Organization.builder()
                .code(code)
                .name(name)
                .description(description)
                .type(type)
                .typeDescription(getTypeDescription(type))
                .tier(tier)
                .tierDescription(getTierDescription(tier))
                .contactEmail(contactEmail)
                .contactPhone(contactPhone)
                .website(website)
                .isHeadquarters(isHeadquarters)
                .isActive(true)
                .isLocked(false)
                .currency("XAF")
                .language("FR")
                .timezone("Africa/Douala")
                .build();
        org.setCreatedBy("SYSTEM_INIT");

        if (parentId != null) {
            Organization parent = organizationRepository.findById(parentId).orElseThrow();
            org.setParentOrganizationId(parent.getId());
            org.setHierarchyLevel(parent.getHierarchyLevel() + 1);
            org.setHierarchyPath(parent.getHierarchyPath() + "/" + code);
            parent.setHasChildren(true);
            organizationRepository.save(parent);
        } else {
            org.setHierarchyLevel(0);
            org.setHierarchyPath(code);
        }

        return organizationRepository.save(org);
    }

    private void createSite(String code, String siteId, String name, String description,
                            String organizationId, String orgId, String type,
                            String addressLine1, String city, String region,
                            Double latitude, Double longitude) {
        Organization org = organizationRepository.findById(organizationId).orElseThrow();

        Site site = Site.builder()
                .code(code)
                .siteId(siteId)
                .name(name)
                .description(description)
                .organizationId(org.getId())
                .orgId(org.getCode())
                .type(type)
                .typeDescription(getSiteTypeDescription(type))
                .addressLine1(addressLine1)
                .city(city)
                .region(region)
                .country("CM")
                .latitude(latitude)
                .longitude(longitude)
                .geofenceRadiusMeters(200)
                .isDefault(false)
                .isActive(true)
                .isLocked(false)
                .disabled(false)
                .isOperational(true)
                .build();
        site.setCreatedBy("SYSTEM_INIT");

        siteRepository.save(site);
    }

    private String getTypeDescription(String typeCode) {
        try {
            return OrganizationType.fromCode(typeCode).getDescription();
        } catch (Exception e) {
            return typeCode;
        }
    }

    private String getTierDescription(String tierCode) {
        try {
            return OrganizationTier.fromCode(tierCode).getDescription();
        } catch (Exception e) {
            return tierCode;
        }
    }

    private String getSiteTypeDescription(String typeCode) {
        try {
            return SiteType.fromCode(typeCode).getDescription();
        } catch (Exception e) {
            return typeCode;
        }
    }
}