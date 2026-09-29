package com.gpl.fleet.config;

import com.gpl.fleet.model.Device;
import com.gpl.fleet.model.Vehicle;
import com.gpl.fleet.model.VehicleTelemetry;
import com.gpl.fleet.repository.DeviceRepository;
import com.gpl.fleet.repository.VehicleRepository;
import com.gpl.fleet.repository.VehicleTelemetryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Seed data for the fleet-device service (dev / test / local profiles only).
 *
 * <p>Creates the rolling stock the national map renders as truck pins:</p>
 * <ul>
 *   <li>6 vehicles with deterministic IDs (so tour seeders and the PDA can
 *       reference them: {@code VEH-MKT-GPL-00x}, {@code VEH-TRP-ABC-00x})</li>
 *   <li>6 GPS tracker devices, one assigned per vehicle, positioned on the
 *       Douala / Yaoundé corridors used by the seeded tours</li>
 *   <li>A 24h telemetry trail per vehicle (30-min steps) from its depot
 *       toward the first seeded checkpoint city, so
 *       {@code GET /telemetry/vehicles/{id}?start&end} and
 *       {@code .../latest} return drawable corridors</li>
 * </ul>
 *
 * <p>Coordinates mirror the organization-service site seeds
 * (Dépôt Principal Douala 4.0511,9.7679 — Bonabéri 4.0930,9.7400 —
 * Dépôt Principal Yaoundé 3.8612,11.5217 — Mvan 3.8280,11.5520).</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "test", "local"})
public class FleetDeviceDataInitializer implements CommandLineRunner {

    private final VehicleRepository vehicleRepository;
    private final DeviceRepository deviceRepository;
    private final VehicleTelemetryRepository telemetryRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Override
    @Transactional
    public void run(String... args) {
        log.info("===== Initialisation des données de test - Flotte & GPS =====");

        if (vehicleRepository.count() > 0) {
            log.info("Des véhicules existent déjà en base. Initialisation ignorée.");
            return;
        }

        List<Vehicle> vehicles = List.of(
                createVehicle("VEH-MKT-GPL-001", "CE-1234-AB", "VRAC", "MKT-GPL", 20.0, null),
                createVehicle("VEH-MKT-GPL-002", "CE-5678-CD", "BOUTEILLES50KG", "MKT-GPL", null, 200),
                createVehicle("VEH-MKT-GPL-003", "CE-2468-EF", "VRAC", "MKT-GPL", 20.0, null),
                createVehicle("VEH-TRP-ABC-001", "LT-9012-GH", "VRAC", "TRP-ABC", 20.0, null),
                createVehicle("VEH-TRP-ABC-002", "LT-3456-IJ", "BOUTEILLES50KG", "TRP-ABC", null, 200),
                createVehicle("VEH-TRP-ABC-003", "LT-7890-KL", "VRAC", "TRP-ABC", 20.0, null));

        // depot -> first-stop corridors, matching the seeded tours
        double[][] doualaCorridor = {{4.0511, 9.7679}, {4.0930, 9.7400}};
        double[][] yaoundeCorridor = {{3.8612, 11.5217}, {3.8280, 11.5520}};

        createDevice("GPS-001", vehicles.get(0), 4.0620, 9.7620, 87, doualaCorridor);
        createDevice("GPS-002", vehicles.get(1), 3.8510, 11.5300, 74, yaoundeCorridor);
        createDevice("GPS-003", vehicles.get(2), 3.8450, 11.5380, 65, yaoundeCorridor);
        createDevice("GPS-004", vehicles.get(3), 4.0750, 9.7550, 92, doualaCorridor);
        createDevice("GPS-005", vehicles.get(4), 4.0480, 9.7020, 58, doualaCorridor);
        createDevice("GPS-006", vehicles.get(5), 4.0100, 9.6850, 81, doualaCorridor);

        log.info("===== Initialisation terminée : {} véhicule(s), {} device(s) =====",
                vehicleRepository.count(), deviceRepository.count());
    }

    private Vehicle createVehicle(String id, String licensePlate, String type,
                                  String organizationId, Double maxVolume, Integer maxBottleCount) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setLicensePlate(licensePlate);
        vehicle.setType(type);
        vehicle.setOrganizationId(organizationId);
        vehicle.setMaxVolume(maxVolume);
        vehicle.setMaxBottleCount(maxBottleCount);
        vehicle.setCertificateNumber("CERT-" + licensePlate);
        vehicle.setCertificateExpiryAt(Instant.now().plus(365, ChronoUnit.DAYS));
        vehicle.setTareWeight("VRAC".equals(type) ? 8.5 : 6.0);
        vehicle.setActive(true);
        vehicle.setCreatedBy("SYSTEM_INIT");
        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Véhicule créé : id={}, plate={}, type={}, org={}",
                saved.getId(), saved.getLicensePlate(), saved.getType(), saved.getOrganizationId());
        return saved;
    }

    private void createDevice(String serial, Vehicle vehicle, double lat, double lng,
                              int batteryLevel, double[][] corridor) {
        Device device = new Device();
        device.setSerialNumber(serial);
        device.setDeviceType("GPS_TRACKER");
        device.setFirmwareVersion("1.4.2");
        device.setBatteryLevel(batteryLevel);
        device.setBatteryCritical(batteryLevel < 20);
        device.setLastSync(Instant.now());
        device.setLastLatitude(lat);
        device.setLastLongitude(lng);
        device.setAssignedToVehicleId(vehicle.getId());
        device.setOrganizationId(vehicle.getOrganizationId());
        device.setCreatedBy("SYSTEM_INIT");
        deviceRepository.save(device);

        seedTelemetryTrail(vehicle.getId(), corridor, batteryLevel);
        log.info("Tracker créé : serial={}, véhicule={}, pos={},{}, batterie={}",
                serial, vehicle.getId(), lat, lng, batteryLevel);
    }

    /**
     * 48 points over the last 24h interpolating depot → first stop, so the
     * map can draw a corridor polyline and a live pin per vehicle.
     */
    private void seedTelemetryTrail(String vehicleId, double[][] corridor, int batteryLevel) {
        Instant now = Instant.now();
        for (int i = 0; i < 48; i++) {
            double t = i / 47.0;
            double lat = corridor[0][0] + t * (corridor[1][0] - corridor[0][0]);
            double lng = corridor[0][1] + t * (corridor[1][1] - corridor[0][1]);
            VehicleTelemetry point = new VehicleTelemetry(
                    vehicleId,
                    now.minus(24, ChronoUnit.HOURS).plus(i * 30L, ChronoUnit.MINUTES),
                    geometryFactory.createPoint(new Coordinate(lng, lat)),
                    t < 0.1 || t > 0.9 ? 0.0 : 45.0 + 20.0 * Math.sin(t * Math.PI),
                    t < 0.5 ? 320.0 : 140.0,
                    Math.min(100.0, batteryLevel + t * 5.0));
            telemetryRepository.save(point);
        }
    }
}
