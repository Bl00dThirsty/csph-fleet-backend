package com.gpl.gateway.error;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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

/**
 * La liste des prefixes routes.
 *
 * <p>Elle n'est plus recopiee a la main. Elle etait dupliquee ici « volontairement »
 * et avait deja derive dans les deux sens : elle/listait
 * {@code /api/v1/transporter-contracts}, qui ne pointe vers aucun controleur, et
 * ignorait {@code /api/v1/scan-events}, qui est le chemin d'ecriture des preuves du
 * PDA. Trois tables de routes maintenues a la main, dont une seule lue — voila
 * pourquoi la route du livreur etait invisible.
 *
 * <p>On lit donc les {@code RouteDefinition} reelles, ce qui rend la derive
 * impossible par construction. Le remplissage est paresseux et tolere l'echec :
 * une page d'erreur ne doit jamais dependre du chargement des routes.
 *
 * @author GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 */
@Component
public class GatewayErrorAttributes extends DefaultErrorAttributes {

    private final RouteDefinitionLocator routeDefinitionLocator;

    private final AtomicReference<Map<String, String>> routedPrefixes = new AtomicReference<>(null);

    public GatewayErrorAttributes(RouteDefinitionLocator routeDefinitionLocator) {
        this.routeDefinitionLocator = routeDefinitionLocator;
    }

    /**
     * Prefixe -> service, reconstruit depuis les routes declarees. Une seule
     * entree par prefixe ; en cas de collision, le premier service gagne et
     * c'est ce que Spring Cloud resoudrait de toute facon.
     */
    private Map<String, String> routedPrefixes() {
        Map<String, String> cached = routedPrefixes.get();
        if (cached != null) {
            return cached;
        }
        Map<String, String> resolved = resolveFromRouteDefinitions();
        routedPrefixes.compareAndSet(null, resolved);
        return routedPrefixes.get();
    }

    private Map<String, String> resolveFromRouteDefinitions() {
        Map<String, String> map = new LinkedHashMap<>();
        List<RouteDefinition> definitions;
        try {
            definitions = routeDefinitionLocator.getRouteDefinitions().collectList().block();
        } catch (RuntimeException unavailable) {
            // Une page d'erreur ne doit pas dependre des routes : on degrade,
            // on ne leve pas.
            return map;
        }
        if (definitions == null) {
            return map;
        }
        definitions.stream()
                .sorted(Comparator.comparing(RouteDefinition::getId))
                .forEach(route -> {
                    String service = serviceNameOf(route);
                    if (service == null) {
                        return;
                    }
                    for (PredicateDefinition predicate : route.getPredicates()) {
                        if (!"Path".equals(predicate.getName())) {
                            continue;
                        }
                        for (String pattern : splitArgs(predicate.getArgs().get("_genkey_0"))) {
                            // Les docs swagger sont routees mais ne sont pas des
                            // prefixes d'API : les lister ici noierait le diagnostic.
                            if (!pattern.startsWith("/api/")) {
                                continue;
                            }
                            map.putIfAbsent(pattern, service);
                        }
                    }
                });
        return map;
    }

    /** `lb://tour-service` -> `tour-service`; une URI nue est prise telle quelle. */
    private static String serviceNameOf(RouteDefinition route) {
        java.net.URI uri = route.getUri();
        if (uri == null) {
            return null;
        }
        String trimmed = uri.toString();
        if (trimmed.startsWith("lb://")) {
            trimmed = trimmed.substring("lb://".length());
        }
        int slash = trimmed.indexOf('/');
        return slash >= 0 ? trimmed.substring(0, slash) : trimmed;
    }

    /**
     * Une valeur de predicat Path est une liste de motifs separes par des
     * virgules : `/api/v1/tours/**, /api/v1/checkpoints/**`.
     */
    private static List<String> splitArgs(String value) {
        List<String> out = new ArrayList<>();
        if (value == null) {
            return out;
        }
        for (String part : value.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
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

    private Map<String, Object> enrich(Map<String, Object> attrs, String method, String path,
                                      Object message) {
        String target = matchService(path);
        attrs.put("method", method);
        attrs.put("path", path);
        attrs.put("message", message);
        attrs.put("targetService", target != null ? target : "aucune route declaree");
        attrs.put("routedPrefixes", new ArrayList<>(routedPrefixes().keySet()));
        attrs.put("hint", buildHint(path, target));
        return attrs;
    }

    private String matchService(String path) {
        for (Map.Entry<String, String> entry : routedPrefixes().entrySet()) {
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
                + "ne mappe pas ce chemin : verifiez son controleur. Les prefixes ci-dessus sont ceux "
                + "que le gateway route reellement — un chemin absent de cette liste n'est route "
                + "nulle part.";
    }

    /** Liste ordonnee et sans doublon des services joignables via le gateway. */
    public List<String> services() {
        List<String> out = new ArrayList<>();
        for (String service : routedPrefixes().values()) {
            if (!out.contains(service)) {
                out.add(service);
            }
        }
        return out;
    }
}
