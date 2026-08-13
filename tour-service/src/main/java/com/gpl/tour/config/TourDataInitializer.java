package com.gpl.tour.config;

import com.gpl.tour.dto.CreateCheckpointDto;
import com.gpl.tour.dto.CreateTourDto;
import com.gpl.tour.dto.TourResponseDto;
import com.gpl.tour.model.Checkpoint;
import com.gpl.tour.model.Tour;
import com.gpl.tour.repository.CheckpointRepository;
import com.gpl.tour.repository.TourRepository;
import com.gpl.tour.service.TourService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Initialiseur de données de test pour le service des tournées.
 * Crée deux tournées de démonstration avec leurs arrêts (checkpoints) clients :
 * <ul>
 *   <li>Tournée VRAC (gaz liquide en camion-citerne, mode EXTERNAL)</li>
 *   <li>Tournée BOUTEILLES50KG (bouteilles 50kg en camion plateau, mode EXTERNAL)</li>
 * </ul>
 *
 * <p>Chaque tournée comporte 3 arrêts chez des clients différents
 * avec des horaires de passage planifiés, illustrant le circuit multi-clients.</p>
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   11.08.2026
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "test", "local", "default"})
@Order(1)
public class TourDataInitializer implements CommandLineRunner {

    private final TourRepository tourRepository;
    private final CheckpointRepository checkpointRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("===== Initialisation des données de test - Tournées =====");

        if (tourRepository.count() > 0) {
            log.info("Des tournées existent déjà en base. Initialisation ignorée.");
            return;
        }

        Tour tourVrac         = createTourVrac();
        Tour tourBouteilles   = createTourBouteilles();

        createCheckpointsTourVrac(tourVrac);
        createCheckpointsTourBouteilles(tourBouteilles);

        log.info("===== Initialisation terminée : {} tournée(s), {} checkpoint(s) créé(s) =====",
                tourRepository.count(), checkpointRepository.count());
    }

    /* =========================================================
     *  TOURNÉE 1 : VRAC (camion-citerne, gaz liquide)
     * =========================================================
     *
     *  Scénario : La société GPL Cameroun (Marketeur TOTAL) organise
     *  une tournée de ravitaillement en gaz liquide VRAC pour 3 clients
     *  industriels de la région de Douala. La tournée est sous-traitée
     *  au transporteur Trans-GPL Express (TRP-ABC).
     *
     *  Véhicule : Camion-citerne 20 tonnes
     *  Quantité totale demandée : 18 000 kg (18 tonnes)
     *  Durée estimée de la tournée : 8 heures
     */
    private Tour createTourVrac() {
        Tour tour = new Tour();
        tour.setTourCode("T-VRAC-2026-001");
        tour.setMarketerOrganizationId("MKT-GPL");
        tour.setExecutionMode("EXTERNAL");
        tour.setTransporterOrganizationId("TRP-ABC");
        /*
         * Véhicule et chauffeur : à affecter par le transporteur lors de l'accusé de réception.
         * Identifiants placeholders — seront mis à jour quand l'entité Vehicle sera disponible.
         */
        tour.setVehicleId(null);
        tour.setDriverId(null);
        tour.setLivreurPersonId(null);
        tour.setType("VRAC");
        tour.setRequestedQuantity(18_000.0);
        tour.setLoadedQuantity(null);
        tour.setDeliveredQuantity(null);
        tour.setStartedAt(null);
        tour.setClosedAt(null);
        tour.setCreatedBy("SYSTEM_INIT");

        Tour saved = tourRepository.save(tour);
        log.info("Tournée VRAC créée : id={}, code={}", saved.getId(), saved.getTourCode());
        return saved;
    }

    /* =========================================================
     *  CHECKPOINTS TOURNÉE VRAC : 3 clients industriels Douala
     * =========================================================
     *
     *  Circuit : Dépôt SONARA → Client 1 (Bonabéri) → Client 2 (Akwa Nord)
     *            → Client 3 (Zone Industrielle Bassa) → Retour dépôt
     *
     *  Les heures de passage prévues sont calculées à partir de maintenant + offset.
     *  En production, le Marketeur saisira des horaires réels.
     */
    private void createCheckpointsTourVrac(Tour tour) {
        Instant now = Instant.now();

        /*
         * Arrêt 1 : INDUSTRIES SARL – Usine de Bonabéri
         * Livraison : 7 000 kg de GPL VRAC
         * Secteur : Bonabéri, Douala IV
         * Contact : Service approvisionnement
         */
        createCheckpoint(tour.getId(), "SITE-CLT-INDUSTRIES-SARL-001",  null,
                1, now.plus(2, ChronoUnit.HOURS));

        /*
         * Arrêt 2 : HÔTELS & RESTAURANTS PLUS – Cuisine centrale Akwa Nord
         * Livraison : 5 000 kg de GPL VRAC
         * Secteur : Akwa Nord, Douala II
         * Contact : Responsable maintenance énergie
         */
        createCheckpoint(tour.getId(), "SITE-CLT-HOTELS-RESTOS-002", null,
                2, now.plus(4, ChronoUnit.HOURS));

        /*
         * Arrêt 3 : PME ÉNERGIE CAMEROUN – Entrepôt Zone Industrielle Bassa
         * Livraison : 6 000 kg de GPL VRAC
         * Secteur : Zone Industrielle de Bassa, Douala V
         * Contact : Gestionnaire de stocks
         */
        createCheckpoint(tour.getId(), "SITE-CLT-PME-ENERGIE-003", null,
                3, now.plus(6, ChronoUnit.HOURS));

        log.info("3 checkpoints VRAC créés pour la tournée id={}", tour.getId());
    }

    /* =========================================================
     *  TOURNÉE 2 : BOUTEILLES 50KG (camion plateau, bouteilles)
     * =========================================================
     *
     *  Scénario : Tournée de livraison de bouteilles GPL de 50 kg pour
     *  3 revendeurs agréés de la région de Yaoundé. La tournée est
     *  sous-traitée au transporteur Trans-GPL Express (TRP-ABC).
     *
     *  Véhicule : Camion plateau 10 tonnes (200 bouteilles de 50 kg)
     *  Quantité totale demandée : 10 000 kg (200 bouteilles × 50 kg)
     *  Durée estimée de la tournée : 7 heures
     */
    private Tour createTourBouteilles() {
        Tour tour = new Tour();
        tour.setTourCode("T-BTL50-2026-001");
        tour.setMarketerOrganizationId("MKT-GPL");
        tour.setExecutionMode("EXTERNAL");
        tour.setTransporterOrganizationId("TRP-ABC");
        tour.setVehicleId(null);
        tour.setDriverId(null);
        tour.setLivreurPersonId(null);
        tour.setType("BOUTEILLES50KG");
        tour.setRequestedQuantity(10_000.0);
        tour.setLoadedQuantity(null);
        tour.setDeliveredQuantity(null);
        tour.setStartedAt(null);
        tour.setClosedAt(null);
        tour.setCreatedBy("SYSTEM_INIT");

        Tour saved = tourRepository.save(tour);
        log.info("Tournée BOUTEILLES50KG créée : id={}, code={}", saved.getId(), saved.getTourCode());
        return saved;
    }

    /* =========================================================
     *  CHECKPOINTS TOURNÉE BOUTEILLES : 3 revendeurs Yaoundé
     * =========================================================
     *
     *  Circuit : Dépôt SCTM Yaoundé → Client 1 (Mvan) → Client 2 (Ngousso)
     *            → Client 3 (Mvog-Mbi) → Retour dépôt
     */
    private void createCheckpointsTourBouteilles(Tour tour) {
        Instant now = Instant.now();

        /*
         * Arrêt 1 : STATION TOTAL MVAN – Revendeur agréé bouteilles 50 kg
         * Livraison : 80 bouteilles (4 000 kg)
         * Secteur : Mvan, Yaoundé VI
         * Contact : Gérant station-service
         */
        createCheckpoint(tour.getId(), "SITE-CLT-TOTAL-MVAN-001", null,
                1, now.plus(2, ChronoUnit.HOURS));

        /*
         * Arrêt 2 : SUPERMARCHÉ NGOUSSO – Point de vente GPL bouteilles
         * Livraison : 60 bouteilles (3 000 kg)
         * Secteur : Ngousso, Yaoundé II
         * Contact : Responsable rayon énergie
         */
        createCheckpoint(tour.getId(), "SITE-CLT-SUPERMARCHE-NGOUSSO-002", null,
                2, now.plus(4, ChronoUnit.HOURS));

        /*
         * Arrêt 3 : CENTRE SOCIAL MVOG-MBI – Distribution GPL subventionné
         * Livraison : 60 bouteilles (3 000 kg)
         * Secteur : Mvog-Mbi, Yaoundé II
         * Contact : Coordonnateur social
         * Note : Livraison prioritaire — programme subvention CSPH
         */
        createCheckpoint(tour.getId(), "SITE-CLT-CENTRE-MVOGMBI-003", null,
                3, now.plus(6, ChronoUnit.HOURS));

        log.info("3 checkpoints BOUTEILLES50KG créés pour la tournée id={}", tour.getId());
    }

    /* =========================================================
     *  Helper : création d'un checkpoint
     * =========================================================
     */
    private void createCheckpoint(String tourId, String clientSiteId, String siteId,
                                   int sequence, Instant expectedArrival) {
        Checkpoint cp = Checkpoint.builder()
                .tourId(tourId)
                .clientSiteId(clientSiteId)
                .siteId(siteId)
                .sequence(sequence)
                .expectedArrival(expectedArrival)
                .build();
        cp.setCreatedBy("SYSTEM_INIT");
        checkpointRepository.save(cp);
        log.debug("Checkpoint seq={} créé pour tourId={} → clientSiteId={}", sequence, tourId, clientSiteId);
    }
}
