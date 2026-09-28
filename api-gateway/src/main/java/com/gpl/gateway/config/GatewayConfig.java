package com.gpl.gateway.config;

import com.gpl.gateway.filter.JwtAuthenticationFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public GatewayConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                // Swagger / OpenAPI routes (no auth required)
                .route("auth-service-swagger", r -> r.path("/v3/api-docs/auth-service/**")
                        .uri("lb://auth-service"))
                .route("organization-service-swagger", r -> r.path("/v3/api-docs/organization-service/**")
                        .uri("lb://organization-service"))
                .route("user-service-swagger", r -> r.path("/v3/api-docs/user-service/**")
                        .uri("lb://user-service"))
                .route("audit-service-swagger", r -> r.path("/v3/api-docs/audit-service/**")
                        .uri("lb://audit-service"))
                .route("notification-service-swagger", r -> r.path("/v3/api-docs/notification-service/**")
                        .uri("lb://notification-service"))
                .route("cylinder-service-swagger", r -> r.path("/v3/api-docs/cylinder-service/**")
                        .uri("lb://cylinder-service"))
                .route("fleet-device-service-swagger", r -> r.path("/v3/api-docs/fleet-device-service/**")
                        .uri("lb://fleet-device-service"))
                .route("tour-service-swagger", r -> r.path("/v3/api-docs/tour-service/**")
                        .uri("lb://tour-service"))
                .route("subsidy-service-swagger", r -> r.path("/v3/api-docs/subsidy-service/**")
                        .uri("lb://subsidy-service"))
                .route("swagger-ui", r -> r.path("/swagger-ui/**", "/swagger-ui.html", "/webjars/**")
                        .uri("lb://auth-service"))

                // Auth endpoints
                .route("auth-service", r -> r.path("/api/v1/auth/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://auth-service"))
                .route("auth-service-me", r -> r.path("/api/v1/me/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://auth-service"))
                // Organization endpoints
                .route("organization-service-orgs", r -> r.path("/api/v1/organizations/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://organization-service"))
                .route("organization-service-sites", r -> r.path("/api/v1/sites/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://organization-service"))
                .route("organization-service-client-sites", r -> r.path("/api/v1/client-sites/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://organization-service"))
                .route("organization-service-classifications", r -> r.path("/api/v1/classifications/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://organization-service"))
                .route("organization-service-rels", r -> r.path("/api/v1/organization-relationships/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://organization-service"))
                // User endpoints
                .route("user-service-persons", r -> r.path("/api/v1/persons/**", "/api/v1/users/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://user-service"))
                .route("user-service-roles", r -> r.path("/api/v1/roles/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://user-service"))
                .route("user-service-permissions", r -> r.path("/api/v1/permissions/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://user-service"))
                .route("user-service-groups", r -> r.path("/api/v1/groups/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://user-service"))
                // Audit endpoints
                .route("audit-service", r -> r.path("/api/v1/audit/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://audit-service"))
                // Notification endpoints
                .route("notification-service-notifications", r -> r.path("/api/v1/notifications/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://notification-service"))
                .route("notification-service-templates", r -> r.path("/api/v1/notification-templates/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://notification-service"))
                // Cylinder endpoints
                .route("cylinder-service-cylinders", r -> r.path("/api/v1/cylinders/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://cylinder-service"))
                .route("cylinder-service-rfid", r -> r.path("/api/v1/rfid/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://cylinder-service"))
                .route("cylinder-service-scans", r -> r.path("/api/v1/scans/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://cylinder-service"))
                // Subsidy endpoints
                .route("subsidy-service-declarations", r -> r.path("/api/v1/declarations/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://subsidy-service"))
                .route("subsidy-service-reconciliations", r -> r.path("/api/v1/reconciliations/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://subsidy-service"))
                .route("subsidy-service-redressements", r -> r.path("/api/v1/redressements/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://subsidy-service"))
                // Fleet Device endpoints
                .route("fleet-device-service-vehicles", r -> r.path("/api/v1/vehicles/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://fleet-device-service"))
                .route("fleet-device-service-devices", r -> r.path("/api/v1/devices/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://fleet-device-service"))
                .route("fleet-device-service-telemetry", r -> r.path("/api/v1/telemetry/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://fleet-device-service"))
                // Tour endpoints
                .route("tour-service-tours", r -> r.path("/api/v1/tours/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://tour-service"))
                .route("tour-service-checkpoints", r -> r.path("/api/v1/checkpoints/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://tour-service"))
                .route("tour-service-pickups", r -> r.path("/api/v1/pickups/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://tour-service"))
                .route("tour-service-contracts", r -> r.path("/api/v1/contracts/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://tour-service"))
                // The livreur PDA's scan write path. Absent from this table entirely
                // until now, so a reader looking for "how does a driver post a
                // scan" found nothing, and the only route for it lived in
                // application.yml pointing at cylinder-service — which serves
                // /api/v1/scans and cannot accept a PDA scan. This controller was
                // written for exactly this caller, bulk resync included.
                .route("tour-service-scan-events", r -> r.path("/api/v1/scan-events/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://tour-service"))
                .build();
    }
}
