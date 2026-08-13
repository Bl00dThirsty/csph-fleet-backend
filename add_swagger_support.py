import os, re

BASE = r'c:\Users\User\Downloads\gpl-rfid-livraisons\backend'

def write_file(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content.strip() + '\n')
    print(f"Created: {path}")

# 1. Add springdoc dependency to POMs
def add_springdoc_dependency(pom_path, is_webflux=False):
    with open(pom_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if 'springdoc-openapi' in content:
        return
    
    artifact = 'springdoc-openapi-starter-webflux-ui' if is_webflux else 'springdoc-openapi-starter-webmvc-ui'
    dep = f"""
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>{artifact}</artifactId>
            <version>${{springdoc.version}}</version>
        </dependency>
    </dependencies>"""
    
    content = content.replace('</dependencies>', dep)
    with open(pom_path, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f"Updated POM: {pom_path}")

webmvc_services = ['tour-service', 'cylinder-service', 'fleet-device-service', 'subsidy-service']
for s in webmvc_services:
    add_springdoc_dependency(os.path.join(BASE, s, 'pom.xml'), is_webflux=False)

add_springdoc_dependency(os.path.join(BASE, 'api-gateway', 'pom.xml'), is_webflux=True)

# 2. Create SwaggerConfig in all microservices
services_config = {
    'auth-service': ('com.gpl.auth.config', 'Auth Service API', '8081'),
    'organization-service': ('com.gpl.organization.config', 'Organization Service API', '8082'),
    'user-service': ('com.gpl.user.config', 'User Service API', '8083'),
    'audit-service': ('com.gpl.audit.config', 'Audit Service API', '8084'),
    'notification-service': ('com.gpl.notification.config', 'Notification Service API', '8085'),
    'tour-service': ('com.gpl.tour.config', 'Tour & Pickup Service API', '8086'),
    'cylinder-service': ('com.gpl.cylinder.config', 'Cylinder & RFID Service API', '8087'),
    'fleet-device-service': ('com.gpl.fleet.config', 'Fleet & IoT Device Service API', '8088'),
    'subsidy-service': ('com.gpl.subsidy.config', 'Subsidy & Reconciliation Service API', '8089')
}

swagger_code_template = """package {package_name};

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {{

    @Bean
    public OpenAPI customOpenAPI() {{
        return new OpenAPI()
                .info(new Info()
                        .title("{title}")
                        .version("1.0.0")
                        .description("Documentation OpenAPI 3.0 & Test d'API interactif pour {title}"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }}
}}
"""

for service, (pkg, title, port) in services_config.items():
    pkg_path = os.path.join(BASE, service, 'src', 'main', 'java', *pkg.split('.'))
    file_path = os.path.join(pkg_path, 'SwaggerConfig.java')
    write_file(file_path, swagger_code_template.format(package_name=pkg, title=title))

# 3. Configure API Gateway Application.yml for Swagger Aggregation
gateway_yml_path = os.path.join(BASE, 'api-gateway', 'src', 'main', 'resources', 'application.yml')

gateway_yml = """server:
  port: 8080

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      routes:
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/v1/auth/**, /v3/api-docs/auth-service
        - id: organization-service
          uri: lb://organization-service
          predicates:
            - Path=/api/v1/organizations/**, /api/v1/sites/**, /api/v1/classifications/**, /api/v1/organization-relationships/**, /api/v1/clients/**, /api/v1/client-sites/**, /api/v1/regions/**, /v3/api-docs/organization-service
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/v1/persons/**, /api/v1/roles/**, /api/v1/permissions/**, /api/v1/groups/**, /api/v1/drivers/**, /api/v1/custom-roles/**, /v3/api-docs/user-service
        - id: audit-service
          uri: lb://audit-service
          predicates:
            - Path=/api/v1/audit/**, /api/v1/settings/**, /v3/api-docs/audit-service
        - id: notification-service
          uri: lb://notification-service
          predicates:
            - Path=/api/v1/notifications/**, /api/v1/notification-templates/**, /api/v1/anomalies/**, /api/v1/reports/**, /v3/api-docs/notification-service
        - id: tour-service
          uri: lb://tour-service
          predicates:
            - Path=/api/v1/tours/**, /api/v1/checkpoints/**, /api/v1/pickup-requests/**, /api/v1/transporter-contracts/**, /v3/api-docs/tour-service
        - id: cylinder-service
          uri: lb://cylinder-service
          predicates:
            - Path=/api/v1/cylinders/**, /api/v1/rfid-tags/**, /api/v1/cylinder-movements/**, /api/v1/scan-events/**, /api/v1/inventory-snapshots/**, /v3/api-docs/cylinder-service
        - id: fleet-device-service
          uri: lb://fleet-device-service
          predicates:
            - Path=/api/v1/vehicles/**, /api/v1/devices/**, /api/v1/vehicle-positions/**, /api/v1/geofence-events/**, /v3/api-docs/fleet-device-service
        - id: subsidy-service
          uri: lb://subsidy-service
          predicates:
            - Path=/api/v1/declarations/**, /api/v1/reconciliations/**, /api/v1/redressements/**, /api/v1/risk-scores/**, /v3/api-docs/subsidy-service

springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    urls:
      - name: 🔐 Auth Service (8081)
        url: /v3/api-docs/auth-service
      - name: 🏢 Organization Service (8082)
        url: /v3/api-docs/organization-service
      - name: 👤 User & Driver Service (8083)
        url: /v3/api-docs/user-service
      - name: 📋 Audit Service (8084)
        url: /v3/api-docs/audit-service
      - name: 🔔 Notification & Anomaly Service (8085)
        url: /v3/api-docs/notification-service
      - name: 🚚 Tour & Pickup Service (8086)
        url: /v3/api-docs/tour-service
      - name: 🏷️ Cylinder & RFID Service (8087)
        url: /v3/api-docs/cylinder-service
      - name: 📡 Fleet & IoT Device Service (8088)
        url: /v3/api-docs/fleet-device-service
      - name: 💰 Subsidy & Reconciliation Service (8089)
        url: /v3/api-docs/subsidy-service

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

gpl:
  jwt:
    secret: ${GPL_JWT_SECRET:gpl-rfid-livraisons-super-secret-key-change-in-production-2024}
"""

write_file(gateway_yml_path, gateway_yml)
print("Swagger configuration completed successfully!")
