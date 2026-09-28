package com.gpl.gateway.error;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Enrichit le dictionnaire d'erreurs du gateway avec de quoi rendre une page
 * d'erreur exploitable : la methode, l'URL, le service que la route devait
 * atteindre, et la liste des prefixes reellement routes.
 *
 * <p>Sans ces champs, la page par defaut de Spring Boot ("Whitelabel Error
 * Page") n'affiche qu'un statut et un horodatage : impossible de savoir quel
 * chemin est mal ecrit ni vers quel microservice il devait partir.
 *
 * <p>Le bean est aussi visible par {@code ErrorWebFluxAutoConfiguration}, qui
 * l'injecte dans son propre gestionnaire par defaut : le JSON du 404 nu change
 * donc lui aussi.
 */
@Component
public class GatewayErrorAttributes extends DefaultErrorAttributes {

    /**
     * Les prefixes declares dans {@code application.yml}
     * (spring.cloud.gateway.routes). Duplique ici volontairement : la liste
     * doit etre lisible dans la page d'erreur, et la source de verite
     * applicative reste le YAML.
     */
    private static final Map<String, String> ROUTED_PREFIXES = new LinkedHashMap<>();

    static {
        ROUTED_PREFIXES.put("/api/v1/auth/**", "auth-service");
        ROUTED_PREFIXES.put("/api/v1/me/**", "auth-service");
        ROUTED_PREFIXES.put("/api/v1/organizations/**", "organization-service");
        ROUTED_PREFIXES.put("/api/v1/sites/**", "organization-service");
        ROUTED_PREFIXES.put("/api/v1/classifications/**", "organization-service");
        ROUTED_PREFIXES.put("/api/v1/organization-relationships/**", "organization-service");
        ROUTED_PREFIXES.put("/api/v1/client-sites/**", "organization-service");
        ROUTED_PREFIXES.put("/api/v1/persons/**", "user-service");
        ROUTED_PREFIXES.put("/api/v1/users/**", "user-service");
        ROUTED_PREFIXES.put("/api/v1/roles/**", "user-service");
        ROUTED_PREFIXES.put("/api/v1/permissions/**", "user-service");
        ROUTED_PREFIXES.put("/api/v1/groups/**", "user-service");
        ROUTED_PREFIXES.put("/api/v1/audit/**", "audit-service");
        ROUTED_PREFIXES.put("/api/v1/notifications/**", "notification-service");
        ROUTED_PREFIXES.put("/api/v1/notification-templates/**", "notification-service");
        ROUTED_PREFIXES.put("/api/v1/tours/**", "tour-service");
        ROUTED_PREFIXES.put("/api/v1/checkpoints/**", "tour-service");
        ROUTED_PREFIXES.put("/api/v1/pickups/**", "tour-service");
        ROUTED_PREFIXES.put("/api/v1/transporter-contracts/**", "tour-service");
        ROUTED_PREFIXES.put("/api/v1/cylinders/**", "cylinder-service");
        ROUTED_PREFIXES.put("/api/v1/rfid/**", "cylinder-service");
        ROUTED_PREFIXES.put("/api/v1/vehicles/**", "fleet-device-service");
        ROUTED_PREFIXES.put("/api/v1/devices/**", "fleet-device-service");
        ROUTED_PREFIXES.put("/api/v1/telemetry/**", "fleet-device-service");
        ROUTED_PREFIXES.put("/api/v1/declarations/**", "subsidy-service");
        ROUTED_PREFIXES.put("/api/v1/reconciliations/**", "subsidy-service");
        ROUTED_PREFIXES.put("/api/v1/redressements/**", "subsidy-service");
    }

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Map<String, Object> attrs = new LinkedHashMap<>(super.getErrorAttributes(request, options));
        return enrich(attrs, request.method().name(),
                request.exchange().getRequest().getPath().value(), attrs.get("message"));
    }

    /**
     * Point d'entree appele par {@link GatewayErrorWebExceptionHandler}, qui
     * travaille sur un {@code ServerWebExchange} et pas sur un
     * {@code ServerRequest}.
     */
    public Map<String, Object> attributesFor(HttpStatusCode status, HttpMethod method, String path,
                                             String message, String traceId) {
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("status", status.value());
        attrs.put("error", status.toString());
        attrs.put("message", message);
        attrs.put("timestamp", Instant.now().toString());
        if (traceId != null) {
            attrs.put("traceId", traceId);
        }
        return enrich(attrs, method.name(), path, message);
    }

    private static Map<String, Object> enrich(Map<String, Object> attrs, String method, String path,
                                              Object message) {
        String target = matchService(path);
        attrs.put("method", method);
        attrs.put("path", path);
        attrs.put("message", message);
        attrs.put("targetService", target != null ? target : "aucune route declaree");
        attrs.put("routedPrefixes", new ArrayList<>(ROUTED_PREFIXES.keySet()));
        attrs.put("hint", buildHint(path, target));
        return attrs;
    }

    private static String matchService(String path) {
        for (Map.Entry<String, String> entry : ROUTED_PREFIXES.entrySet()) {
            String prefix = entry.getKey();
            if (prefix.endsWith("/**")) {
                String bare = prefix.substring(0, prefix.length() - 3);
                if (path.equals(bare) || path.startsWith(bare + "/")) {
                    return entry.getValue();
                }
            } else if (path.equals(prefix)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String buildHint(String path, String target) {
        if ("/".equals(path)) {
            return "La racine du gateway n'est pas une route. Utilisez /swagger-ui.html pour la "
                    + "documentation interactive, ou /api/v1/... pour l'API. L'application web est "
                    + "servie separement par Vite sur le port 5180.";
        }
        if (target == null) {
            return "Aucun prefixe de route ne correspond a cette URL. Verifiez le chemin dans "
                    + "spring.cloud.gateway.routes (api-gateway/src/main/resources/application.yml).";
        }
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
            return "Documentation swagger. Si le document est vide, le service cible n'expose aucune "
                    + "operation springdoc.";
        }
        return "La route existe et pointe vers " + target + ". Un 404 ici signifie que le microservice "
                + "ne mappe pas ce chemin : verifiez son controleur (par exemple /api/v1/rfid-tags "
                + "n'existe pas, c'est /api/v1/rfid).";
    }

    /** Liste ordonnee et sans doublon des services joignables via le gateway. */
    public static List<String> services() {
        List<String> out = new ArrayList<>();
        for (String service : ROUTED_PREFIXES.values()) {
            if (!out.contains(service)) {
                out.add(service);
            }
        }
        return out;
    }
}
