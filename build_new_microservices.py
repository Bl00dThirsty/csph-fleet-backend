import os

BASE = r'c:\Users\User\Downloads\gpl-rfid-livraisons\backend'

def write_file(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content.strip() + '\n')
    print(f"Created: {path}")

def make_pom(module_name, port):
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
# 1. TOUR-SERVICE
# ==========================================
write_file(os.path.join(BASE, 'tour-service', 'pom.xml'), make_pom('tour-service', 8086))
write_file(os.path.join(BASE, 'tour-service', 'src', 'main', 'resources', 'application.yml'), make_yml('tour-service', 8086, 'gpl_tour_db'))

write_file(os.path.join(BASE, 'tour-service', 'src', 'main', 'java', 'com', 'gpl', 'tour', 'TourServiceApplication.java'), """
package com.gpl.tour;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class TourServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TourServiceApplication.class, args);
    }
}
""")

# Tour Entities
write_file(os.path.join(BASE, 'tour-service', 'src', 'main', 'java', 'com', 'gpl', 'tour', 'model', 'Tour.java'), """
package com.gpl.tour.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tours")
public class Tour extends AuditableEntity {
    private String tourCode;
    private String marketerOrganizationId;
    private String executionMode;
    private String transporterOrganizationId;
    private String vehicleId;
    private String driverId;
    private String livreurPersonId;
    private String assignedByTransporterPersonId;
    private Instant transporterAssignedAt;
    private String type;
    private double requestedQuantity;
    private Double loadedQuantity;
    private Double deliveredQuantity;
    private Instant startedAt;
    private Instant closedAt;

    public String getTourCode() { return tourCode; }
    public void setTourCode(String tourCode) { this.tourCode = tourCode; }
    public String getMarketerOrganizationId() { return marketerOrganizationId; }
    public void setMarketerOrganizationId(String marketerOrganizationId) { this.marketerOrganizationId = marketerOrganizationId; }
    public String getExecutionMode() { return executionMode; }
    public void setExecutionMode(String executionMode) { this.executionMode = executionMode; }
    public String getTransporterOrganizationId() { return transporterOrganizationId; }
    public void setTransporterOrganizationId(String transporterOrganizationId) { this.transporterOrganizationId = transporterOrganizationId; }
    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }
    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }
    public String getLivreurPersonId() { return livreurPersonId; }
    public void setLivreurPersonId(String livreurPersonId) { this.livreurPersonId = livreurPersonId; }
    public String getAssignedByTransporterPersonId() { return assignedByTransporterPersonId; }
    public void setAssignedByTransporterPersonId(String assignedByTransporterPersonId) { this.assignedByTransporterPersonId = assignedByTransporterPersonId; }
    public Instant getTransporterAssignedAt() { return transporterAssignedAt; }
    public void setTransporterAssignedAt(Instant transporterAssignedAt) { this.transporterAssignedAt = transporterAssignedAt; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public double getRequestedQuantity() { return requestedQuantity; }
    public void setRequestedQuantity(double requestedQuantity) { this.requestedQuantity = requestedQuantity; }
    public Double getLoadedQuantity() { return loadedQuantity; }
    public void setLoadedQuantity(Double loadedQuantity) { this.loadedQuantity = loadedQuantity; }
    public Double getDeliveredQuantity() { return deliveredQuantity; }
    public void setDeliveredQuantity(Double deliveredQuantity) { this.deliveredQuantity = deliveredQuantity; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
}
""")

write_file(os.path.join(BASE, 'tour-service', 'src', 'main', 'java', 'com', 'gpl', 'tour', 'model', 'Checkpoint.java'), """
package com.gpl.tour.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "checkpoints")
public class Checkpoint extends AuditableEntity {
    private String tourId;
    private String siteId;
    private String clientSiteId;
    private int sequence;
    private Instant expectedArrival;
    private Instant actualArrival;
    private String skipReason;

    public String getTourId() { return tourId; }
    public void setTourId(String tourId) { this.tourId = tourId; }
    public String getSiteId() { return siteId; }
    public void setSiteId(String siteId) { this.siteId = siteId; }
    public String getClientSiteId() { return clientSiteId; }
    public void setClientSiteId(String clientSiteId) { this.clientSiteId = clientSiteId; }
    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }
    public Instant getExpectedArrival() { return expectedArrival; }
    public void setExpectedArrival(Instant expectedArrival) { this.expectedArrival = expectedArrival; }
    public Instant getActualArrival() { return actualArrival; }
    public void setActualArrival(Instant actualArrival) { this.actualArrival = actualArrival; }
    public String getSkipReason() { return skipReason; }
    public void setSkipReason(String skipReason) { this.skipReason = skipReason; }
}
""")

write_file(os.path.join(BASE, 'tour-service', 'src', 'main', 'java', 'com', 'gpl', 'tour', 'model', 'TransporterContract.java'), """
package com.gpl.tour.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "transporter_contracts")
public class TransporterContract extends AuditableEntity {
    private String marketerOrganizationId;
    private String transporterOrganizationId;
    private boolean isPrimary;
    private String contractReference;
    private Instant startedAt;
    private Instant endedAt;
    private boolean isActive = true;

    public String getMarketerOrganizationId() { return marketerOrganizationId; }
    public void setMarketerOrganizationId(String marketerOrganizationId) { this.marketerOrganizationId = marketerOrganizationId; }
    public String getTransporterOrganizationId() { return transporterOrganizationId; }
    public void setTransporterOrganizationId(String transporterOrganizationId) { this.transporterOrganizationId = transporterOrganizationId; }
    public boolean isPrimary() { return isPrimary; }
    public void setPrimary(boolean primary) { isPrimary = primary; }
    public String getContractReference() { return contractReference; }
    public void setContractReference(String contractReference) { this.contractReference = contractReference; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
""")

write_file(os.path.join(BASE, 'tour-service', 'src', 'main', 'java', 'com', 'gpl', 'tour', 'model', 'PickupRequest.java'), """
package com.gpl.tour.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "pickup_requests")
public class PickupRequest extends AuditableEntity {
    private String marketerOrganizationId;
    private String sourceSiteId;
    private String destinationSiteId;
    private double requestedQuantity;
    private Double approvedQuantity;

    public String getMarketerOrganizationId() { return marketerOrganizationId; }
    public void setMarketerOrganizationId(String marketerOrganizationId) { this.marketerOrganizationId = marketerOrganizationId; }
    public String getSourceSiteId() { return sourceSiteId; }
    public void setSourceSiteId(String sourceSiteId) { this.sourceSiteId = sourceSiteId; }
    public String getDestinationSiteId() { return destinationSiteId; }
    public void setDestinationSiteId(String destinationSiteId) { this.destinationSiteId = destinationSiteId; }
    public double getRequestedQuantity() { return requestedQuantity; }
    public void setRequestedQuantity(double requestedQuantity) { this.requestedQuantity = requestedQuantity; }
    public Double getApprovedQuantity() { return approvedQuantity; }
    public void setApprovedQuantity(Double approvedQuantity) { this.approvedQuantity = approvedQuantity; }
}
""")

# Repositories & Controllers Tour Service
write_file(os.path.join(BASE, 'tour-service', 'src', 'main', 'java', 'com', 'gpl', 'tour', 'repository', 'TourRepository.java'), """
package com.gpl.tour.repository;

import com.gpl.tour.model.Tour;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TourRepository extends JpaRepository<Tour, String> {
    List<Tour> findByMarketerOrganizationId(String orgId);
    List<Tour> findByTransporterOrganizationId(String orgId);
}
""")

write_file(os.path.join(BASE, 'tour-service', 'src', 'main', 'java', 'com', 'gpl', 'tour', 'repository', 'CheckpointRepository.java'), """
package com.gpl.tour.repository;

import com.gpl.tour.model.Checkpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CheckpointRepository extends JpaRepository<Checkpoint, String> {
    List<Checkpoint> findByTourIdOrderBySequenceAsc(String tourId);
}
""")

write_file(os.path.join(BASE, 'tour-service', 'src', 'main', 'java', 'com', 'gpl', 'tour', 'controller', 'TourController.java'), """
package com.gpl.tour.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.tour.model.Tour;
import com.gpl.tour.repository.TourRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tours")
public class TourController {
    private final TourRepository tourRepository;

    public TourController(TourRepository tourRepository) {
        this.tourRepository = tourRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Tour>>> listTours() {
        return ResponseEntity.ok(ApiResponse.ok(tourRepository.findAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Tour>> createTour(@RequestBody Tour tour) {
        return ResponseEntity.ok(ApiResponse.ok(tourRepository.save(tour)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Tour>> getTour(@PathVariable String id) {
        return tourRepository.findById(id)
                .map(t -> ResponseEntity.ok(ApiResponse.ok(t)))
                .orElse(ResponseEntity.notFound().build());
    }
}
""")

print("Tour service generated!")
