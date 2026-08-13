import os

BASE = r'c:\Users\User\Downloads\gpl-rfid-livraisons\backend'

def write_file(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content.strip() + '\n')
    print(f"Created: {path}")

def make_pom(module_name):
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.gpl</groupId>
        <artifactId>gpl-rfid-backend</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>{module_name}</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.gpl</groupId>
            <artifactId>common-lib</artifactId>
            <version>${{project.version}}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
"""

def make_yml(app_name, port, db_name):
    return f"""server:
  port: {port}

spring:
  application:
    name: {app_name}
  datasource:
    url: jdbc:postgresql://localhost:5432/{db_name}
    username: ${{DB_USERNAME:postgres}}
    password: ${{DB_PASSWORD:postgres}}
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
"""

# ==========================================
# 2. CYLINDER-SERVICE (8087)
# ==========================================
write_file(os.path.join(BASE, 'cylinder-service', 'pom.xml'), make_pom('cylinder-service'))
write_file(os.path.join(BASE, 'cylinder-service', 'src', 'main', 'resources', 'application.yml'), make_yml('cylinder-service', 8087, 'gpl_cylinder_db'))

write_file(os.path.join(BASE, 'cylinder-service', 'src', 'main', 'java', 'com', 'gpl', 'cylinder', 'CylinderServiceApplication.java'), """
package com.gpl.cylinder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class CylinderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CylinderServiceApplication.class, args);
    }
}
""")

# Cylinder models
write_file(os.path.join(BASE, 'cylinder-service', 'src', 'main', 'java', 'com', 'gpl', 'cylinder', 'model', 'Cylinder.java'), """
package com.gpl.cylinder.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "cylinders")
public class Cylinder extends AuditableEntity {
    private String serialNumber;
    private String barcode;
    private String cylinderTypeId;
    private String ownerOrganizationId;
    private String currentHolderOrganizationId;
    private String currentSiteId;
    private String fillStatus;

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getCylinderTypeId() { return cylinderTypeId; }
    public void setCylinderTypeId(String cylinderTypeId) { this.cylinderTypeId = cylinderTypeId; }
    public String getOwnerOrganizationId() { return ownerOrganizationId; }
    public void setOwnerOrganizationId(String ownerOrganizationId) { this.ownerOrganizationId = ownerOrganizationId; }
    public String getCurrentHolderOrganizationId() { return currentHolderOrganizationId; }
    public void setCurrentHolderOrganizationId(String currentHolderOrganizationId) { this.currentHolderOrganizationId = currentHolderOrganizationId; }
    public String getCurrentSiteId() { return currentSiteId; }
    public void setCurrentSiteId(String currentSiteId) { this.currentSiteId = currentSiteId; }
    public String getFillStatus() { return fillStatus; }
    public void setFillStatus(String fillStatus) { this.fillStatus = fillStatus; }
}
""")

write_file(os.path.join(BASE, 'cylinder-service', 'src', 'main', 'java', 'com', 'gpl', 'cylinder', 'model', 'RfidTag.java'), """
package com.gpl.cylinder.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "rfid_tags")
public class RfidTag extends AuditableEntity {
    private String tagUid;
    private String bottleSerial;
    private String currentSiteId;
    private String currentClientSiteId;

    public String getTagUid() { return tagUid; }
    public void setTagUid(String tagUid) { this.tagUid = tagUid; }
    public String getBottleSerial() { return bottleSerial; }
    public void setBottleSerial(String bottleSerial) { this.bottleSerial = bottleSerial; }
    public String getCurrentSiteId() { return currentSiteId; }
    public void setCurrentSiteId(String currentSiteId) { this.currentSiteId = currentSiteId; }
    public String getCurrentClientSiteId() { return currentClientSiteId; }
    public void setCurrentClientSiteId(String currentClientSiteId) { this.currentClientSiteId = currentClientSiteId; }
}
""")

write_file(os.path.join(BASE, 'cylinder-service', 'src', 'main', 'java', 'com', 'gpl', 'cylinder', 'model', 'ScanEvent.java'), """
package com.gpl.cylinder.model;

import com.gpl.common.model.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "scan_events")
public class ScanEvent extends BaseEntity {
    private String checkpointId;
    private String livreurPersonId;
    private String rfidTagId;
    private String direction;
    private Double latitude;
    private Double longitude;
    private Instant timestamp;
    private Double meterReading;
    private String photoUrl;
    private String pdaSyncId;
    private String conflictStatus;

    public String getCheckpointId() { return checkpointId; }
    public void setCheckpointId(String checkpointId) { this.checkpointId = checkpointId; }
    public String getLivreurPersonId() { return livreurPersonId; }
    public void setLivreurPersonId(String livreurPersonId) { this.livreurPersonId = livreurPersonId; }
    public String getRfidTagId() { return rfidTagId; }
    public void setRfidTagId(String rfidTagId) { this.rfidTagId = rfidTagId; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public Double getMeterReading() { return meterReading; }
    public void setMeterReading(Double meterReading) { this.meterReading = meterReading; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public String getPdaSyncId() { return pdaSyncId; }
    public void setPdaSyncId(String pdaSyncId) { this.pdaSyncId = pdaSyncId; }
    public String getConflictStatus() { return conflictStatus; }
    public void setConflictStatus(String conflictStatus) { this.conflictStatus = conflictStatus; }
}
""")

write_file(os.path.join(BASE, 'cylinder-service', 'src', 'main', 'java', 'com', 'gpl', 'cylinder', 'repository', 'CylinderRepository.java'), """
package com.gpl.cylinder.repository;

import com.gpl.cylinder.model.Cylinder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CylinderRepository extends JpaRepository<Cylinder, String> {
    Optional<Cylinder> findBySerialNumber(String serialNumber);
}
""")

write_file(os.path.join(BASE, 'cylinder-service', 'src', 'main', 'java', 'com', 'gpl', 'cylinder', 'controller', 'CylinderController.java'), """
package com.gpl.cylinder.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.cylinder.model.Cylinder;
import com.gpl.cylinder.repository.CylinderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cylinders")
public class CylinderController {
    private final CylinderRepository cylinderRepository;

    public CylinderController(CylinderRepository cylinderRepository) {
        this.cylinderRepository = cylinderRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Cylinder>>> listCylinders() {
        return ResponseEntity.ok(ApiResponse.ok(cylinderRepository.findAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Cylinder>> createCylinder(@RequestBody Cylinder cylinder) {
        return ResponseEntity.ok(ApiResponse.ok(cylinderRepository.save(cylinder)));
    }
}
""")

# ==========================================
# 3. FLEET-DEVICE-SERVICE (8088)
# ==========================================
write_file(os.path.join(BASE, 'fleet-device-service', 'pom.xml'), make_pom('fleet-device-service'))
write_file(os.path.join(BASE, 'fleet-device-service', 'src', 'main', 'resources', 'application.yml'), make_yml('fleet-device-service', 8088, 'gpl_fleet_db'))

write_file(os.path.join(BASE, 'fleet-device-service', 'src', 'main', 'java', 'com', 'gpl', 'fleet', 'FleetDeviceServiceApplication.java'), """
package com.gpl.fleet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class FleetDeviceServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FleetDeviceServiceApplication.class, args);
    }
}
""")

write_file(os.path.join(BASE, 'fleet-device-service', 'src', 'main', 'java', 'com', 'gpl', 'fleet', 'model', 'Vehicle.java'), """
package com.gpl.fleet.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "vehicles")
public class Vehicle extends AuditableEntity {
    private String licensePlate;
    private String type;
    private String organizationId;
    private Double maxVolume;
    private Integer maxBottleCount;
    private String certificateUrl;
    private String certificateNumber;
    private Instant certificateExpiryAt;
    private Double tareWeight;
    private boolean isActive = true;

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    public Double getMaxVolume() { return maxVolume; }
    public void setMaxVolume(Double maxVolume) { this.maxVolume = maxVolume; }
    public Integer getMaxBottleCount() { return maxBottleCount; }
    public void setMaxBottleCount(Integer maxBottleCount) { this.maxBottleCount = maxBottleCount; }
    public String getCertificateUrl() { return certificateUrl; }
    public void setCertificateUrl(String certificateUrl) { this.certificateUrl = certificateUrl; }
    public String getCertificateNumber() { return certificateNumber; }
    public void setCertificateNumber(String certificateNumber) { this.certificateNumber = certificateNumber; }
    public Instant getCertificateExpiryAt() { return certificateExpiryAt; }
    public void setCertificateExpiryAt(Instant certificateExpiryAt) { this.certificateExpiryAt = certificateExpiryAt; }
    public Double getTareWeight() { return tareWeight; }
    public void setTareWeight(Double tareWeight) { this.tareWeight = tareWeight; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
""")

write_file(os.path.join(BASE, 'fleet-device-service', 'src', 'main', 'java', 'com', 'gpl', 'fleet', 'model', 'Device.java'), """
package com.gpl.fleet.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "devices")
public class Device extends AuditableEntity {
    private String serialNumber;
    private String deviceType;
    private String firmwareVersion;
    private Integer batteryLevel;
    private boolean batteryCritical;
    private Instant lastSync;
    private Double lastLatitude;
    private Double lastLongitude;
    private String assignedToPersonId;
    private String assignedToVehicleId;
    private String organizationId;

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public String getFirmwareVersion() { return firmwareVersion; }
    public void setFirmwareVersion(String firmwareVersion) { this.firmwareVersion = firmwareVersion; }
    public Integer getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(Integer batteryLevel) { this.batteryLevel = batteryLevel; }
    public boolean isBatteryCritical() { return batteryCritical; }
    public void setBatteryCritical(boolean batteryCritical) { this.batteryCritical = batteryCritical; }
    public Instant getLastSync() { return lastSync; }
    public void setLastSync(Instant lastSync) { this.lastSync = lastSync; }
    public Double getLastLatitude() { return lastLatitude; }
    public void setLastLatitude(Double lastLatitude) { this.lastLatitude = lastLatitude; }
    public Double getLastLongitude() { return lastLongitude; }
    public void setLastLongitude(Double lastLongitude) { this.lastLongitude = lastLongitude; }
    public String getAssignedToPersonId() { return assignedToPersonId; }
    public void setAssignedToPersonId(String assignedToPersonId) { this.assignedToPersonId = assignedToPersonId; }
    public String getAssignedToVehicleId() { return assignedToVehicleId; }
    public void setAssignedToVehicleId(String assignedToVehicleId) { this.assignedToVehicleId = assignedToVehicleId; }
    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
}
""")

write_file(os.path.join(BASE, 'fleet-device-service', 'src', 'main', 'java', 'com', 'gpl', 'fleet', 'repository', 'VehicleRepository.java'), """
package com.gpl.fleet.repository;

import com.gpl.fleet.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, String> {
    Optional<Vehicle> findByLicensePlate(String licensePlate);
}
""")

write_file(os.path.join(BASE, 'fleet-device-service', 'src', 'main', 'java', 'com', 'gpl', 'fleet', 'controller', 'VehicleController.java'), """
package com.gpl.fleet.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.fleet.model.Vehicle;
import com.gpl.fleet.repository.VehicleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {
    private final VehicleRepository vehicleRepository;

    public VehicleController(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Vehicle>>> listVehicles() {
        return ResponseEntity.ok(ApiResponse.ok(vehicleRepository.findAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Vehicle>> createVehicle(@RequestBody Vehicle vehicle) {
        return ResponseEntity.ok(ApiResponse.ok(vehicleRepository.save(vehicle)));
    }
}
""")

# ==========================================
# 4. SUBSIDY-SERVICE (8089)
# ==========================================
write_file(os.path.join(BASE, 'subsidy-service', 'pom.xml'), make_pom('subsidy-service'))
write_file(os.path.join(BASE, 'subsidy-service', 'src', 'main', 'resources', 'application.yml'), make_yml('subsidy-service', 8089, 'gpl_subsidy_db'))

write_file(os.path.join(BASE, 'subsidy-service', 'src', 'main', 'java', 'com', 'gpl', 'subsidy', 'SubsidyServiceApplication.java'), """
package com.gpl.subsidy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class SubsidyServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SubsidyServiceApplication.class, args);
    }
}
""")

write_file(os.path.join(BASE, 'subsidy-service', 'src', 'main', 'java', 'com', 'gpl', 'subsidy', 'model', 'Declaration.java'), """
package com.gpl.subsidy.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "declarations")
public class Declaration extends AuditableEntity {
    private String marketerOrganizationId;
    private Instant periodStart;
    private Instant periodEnd;
    private double declaredVolume;
    private String submittedByPersonId;

    public String getMarketerOrganizationId() { return marketerOrganizationId; }
    public void setMarketerOrganizationId(String marketerOrganizationId) { this.marketerOrganizationId = marketerOrganizationId; }
    public Instant getPeriodStart() { return periodStart; }
    public void setPeriodStart(Instant periodStart) { this.periodStart = periodStart; }
    public Instant getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(Instant periodEnd) { this.periodEnd = periodEnd; }
    public double getDeclaredVolume() { return declaredVolume; }
    public void setDeclaredVolume(double declaredVolume) { this.declaredVolume = declaredVolume; }
    public String getSubmittedByPersonId() { return submittedByPersonId; }
    public void setSubmittedByPersonId(String submittedByPersonId) { this.submittedByPersonId = submittedByPersonId; }
}
""")

write_file(os.path.join(BASE, 'subsidy-service', 'src', 'main', 'java', 'com', 'gpl', 'subsidy', 'model', 'Reconciliation.java'), """
package com.gpl.subsidy.model;

import com.gpl.common.model.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "reconciliations")
public class Reconciliation extends BaseEntity {
    private String declarationId;
    private double trackedVolume;
    private Integer trackedBottlesOut;
    private Integer trackedBottlesIn;
    private double volumeGap;
    private double subsidyImpact;
    private String status;
    private String verifiedByPersonId;
    private Instant verifiedAt;
    private String notes;

    public String getDeclarationId() { return declarationId; }
    public void setDeclarationId(String declarationId) { this.declarationId = declarationId; }
    public double getTrackedVolume() { return trackedVolume; }
    public void setTrackedVolume(double trackedVolume) { this.trackedVolume = trackedVolume; }
    public Integer getTrackedBottlesOut() { return trackedBottlesOut; }
    public void setTrackedBottlesOut(Integer trackedBottlesOut) { this.trackedBottlesOut = trackedBottlesOut; }
    public Integer getTrackedBottlesIn() { return trackedBottlesIn; }
    public void setTrackedBottlesIn(Integer trackedBottlesIn) { this.trackedBottlesIn = trackedBottlesIn; }
    public double getVolumeGap() { return volumeGap; }
    public void setVolumeGap(double volumeGap) { this.volumeGap = volumeGap; }
    public double getSubsidyImpact() { return subsidyImpact; }
    public void setSubsidyImpact(double subsidyImpact) { this.subsidyImpact = subsidyImpact; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getVerifiedByPersonId() { return verifiedByPersonId; }
    public void setVerifiedByPersonId(String verifiedByPersonId) { this.verifiedByPersonId = verifiedByPersonId; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
""")

write_file(os.path.join(BASE, 'subsidy-service', 'src', 'main', 'java', 'com', 'gpl', 'subsidy', 'model', 'Redressement.java'), """
package com.gpl.subsidy.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "redressements")
public class Redressement extends AuditableEntity {
    private String reconciliationId;
    private double amount;
    private Instant issuedAt;
    private Instant dueDate;
    private Instant paidAt;
    private String transactionRef;

    public String getReconciliationId() { return reconciliationId; }
    public void setReconciliationId(String reconciliationId) { this.reconciliationId = reconciliationId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }
    public Instant getDueDate() { return dueDate; }
    public void setDueDate(Instant dueDate) { this.dueDate = dueDate; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
}
""")

write_file(os.path.join(BASE, 'subsidy-service', 'src', 'main', 'java', 'com', 'gpl', 'subsidy', 'repository', 'DeclarationRepository.java'), """
package com.gpl.subsidy.repository;

import com.gpl.subsidy.model.Declaration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeclarationRepository extends JpaRepository<Declaration, String> {
    List<Declaration> findByMarketerOrganizationId(String orgId);
}
""")

write_file(os.path.join(BASE, 'subsidy-service', 'src', 'main', 'java', 'com', 'gpl', 'subsidy', 'controller', 'DeclarationController.java'), """
package com.gpl.subsidy.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.subsidy.model.Declaration;
import com.gpl.subsidy.repository.DeclarationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/declarations")
public class DeclarationController {
    private final DeclarationRepository declarationRepository;

    public DeclarationController(DeclarationRepository declarationRepository) {
        this.declarationRepository = declarationRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Declaration>>> listDeclarations() {
        return ResponseEntity.ok(ApiResponse.ok(declarationRepository.findAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Declaration>> createDeclaration(@RequestBody Declaration declaration) {
        return ResponseEntity.ok(ApiResponse.ok(declarationRepository.save(declaration)));
    }
}
""")

print("Cylinder, Fleet-Device, and Subsidy services generated!")
