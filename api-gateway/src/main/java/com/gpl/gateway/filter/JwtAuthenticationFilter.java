package com.gpl.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Value("${gpl.jwt.secret:gpl-rfid-livraisons-super-secret-key-change-in-production-2024}")
    private String jwtSecret;

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    public static class Config {
    }

    private static final List<String> OPEN_ENDPOINTS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/v3/api-docs",
            "/v3/api-docs/",
            "/swagger-ui",
            "/swagger-ui/",
            "/swagger-ui.html",
            "/webjars/"
    );

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (isSecured(request)) {
                if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    return onError(exchange, "Missing Authorization Header", HttpStatus.UNAUTHORIZED);
                }

                String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    return onError(exchange, "Invalid Authorization Header", HttpStatus.UNAUTHORIZED);
                }

                String token = authHeader.substring(7);
                try {
                    SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
                    
                    Claims claims = Jwts.parser()
                            .verifyWith(secretKey)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();

                    String personId = claims.getSubject();
                    String username = claims.get("username", String.class);
                    String orgId = claims.get("orgId", String.class);

                    // Extract roles and permissions from JWT
                    List<String> roles = claims.get("roles", List.class);
                    List<String> permissions = claims.get("permissions", List.class);
                    String rolesStr = (roles != null) ? String.join(",", roles) : "";
                    String permissionsStr = (permissions != null) ? String.join(",", permissions) : "";

                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header("X-User-Username", username != null ? username : "")
                            .header("X-User-PersonId", personId != null ? personId : "")
                            .header("X-User-OrgId", orgId != null ? orgId : "")
                            .header("X-User-Roles", rolesStr)
                            .header("X-User-Permissions", permissionsStr)
                            // Strip the bearer token before forwarding. Downstream
                            // services authenticate exclusively off the X-User-*
                            // headers above (see GplSecurityContextFilter) and never
                            // read Authorization — but the JWT carries the full
                            // permission union (SUPERADMIN: ~190 codes, ~5KB) and
                            // re-forwarding it alongside X-User-Permissions pushed
                            // fat roles over Tomcat's 8KB header cap (HTTP 400 on
                            // every guarded call). The gateway has already
                            // validated the token here; nothing downstream needs it.
                            .headers(headers -> headers.remove(HttpHeaders.AUTHORIZATION))
                            .build();

                    exchange = exchange.mutate().request(mutatedRequest).build();

                } catch (Exception e) {
                    return onError(exchange, "Invalid JWT Token", HttpStatus.UNAUTHORIZED);
                }
            }

            return chain.filter(exchange);
        };
    }

    private boolean isSecured(ServerHttpRequest request) {
        if (org.springframework.http.HttpMethod.OPTIONS.equals(request.getMethod())) {
            return false;
        }
        String path = request.getURI().getPath();
        return OPEN_ENDPOINTS.stream().noneMatch(path::startsWith);
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        byte[] bytes = err.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
