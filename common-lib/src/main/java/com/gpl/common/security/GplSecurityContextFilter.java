package com.gpl.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Filtre Servlet qui lit les headers de sécurité injectés par l'API Gateway
 * et peuple le {@link GplSecurityContext} ThreadLocal pour la durée de la requête.
 *
 * <p>Headers lus :</p>
 * <ul>
 *   <li>{@code X-User-PersonId} — Identifiant de la personne</li>
 *   <li>{@code X-User-Username} — Nom d'utilisateur</li>
 *   <li>{@code X-User-OrgId} — Identifiant de l'organisation</li>
 *   <li>{@code X-User-Roles} — Rôles séparés par des virgules</li>
 *   <li>{@code X-User-Permissions} — Permissions séparées par des virgules</li>
 * </ul>
 *
 * <p>Ce filtre est enregistré automatiquement par {@link GplSecurityAutoConfiguration}
 * uniquement dans les applications Servlet (pas WebFlux / Gateway).</p>
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   13.08.2026
 */
@Slf4j
public class GplSecurityContextFilter extends OncePerRequestFilter {

    public static final String HEADER_PERSON_ID = "X-User-PersonId";
    public static final String HEADER_USERNAME = "X-User-Username";
    public static final String HEADER_ORG_ID = "X-User-OrgId";
    public static final String HEADER_ROLES = "X-User-Roles";
    public static final String HEADER_PERMISSIONS = "X-User-Permissions";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        try {
            String personId = request.getHeader(HEADER_PERSON_ID);
            String username = request.getHeader(HEADER_USERNAME);
            String orgId = request.getHeader(HEADER_ORG_ID);
            Set<String> roles = parseCommaSeparated(request.getHeader(HEADER_ROLES));
            Set<String> permissions = parseCommaSeparated(request.getHeader(HEADER_PERMISSIONS));

            if (personId != null && !personId.isEmpty()) {
                GplSecurityContext.set(new GplSecurityContext.SecurityData(
                        personId, username, orgId, roles, permissions
                ));
                log.trace("SecurityContext set for personId={}, orgId={}, roles={}, perms={}",
                        personId, orgId, roles.size(), permissions.size());
            }

            filterChain.doFilter(request, response);
        } finally {
            // CRITIQUE : toujours nettoyer le ThreadLocal pour éviter les fuites
            // dans les pools de threads (Tomcat, Undertow, etc.)
            GplSecurityContext.clear();
        }
    }

    /**
     * Parse une chaîne séparée par des virgules en un Set de valeurs non-vides.
     * Retourne un ensemble vide si l'entrée est null ou vide.
     */
    private Set<String> parseCommaSeparated(String header) {
        if (header == null || header.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
