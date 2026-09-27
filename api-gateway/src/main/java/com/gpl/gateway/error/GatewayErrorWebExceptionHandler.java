package com.gpl.gateway.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Remplace la "Whitelabel Error Page" de Spring Boot par une page lisible.
 *
 * <p>Deux formats selon le client :
 * <ul>
 *   <li>navigateur ({@code Accept: text/html}) : page HTML autonome, sans
 *       dependance a des ressources externes, listant l'URL fautive, le
 *       microservice que la route devait atteindre, la cause et la liste des
 *       prefixes routes ;</li>
 *   <li>appel d'API ({@code Accept: application/json} ou joker) : le meme
 *       dictionnaire en JSON, ce qui preserve ce que les clients
 *       automatises consommaient.</li>
 * </ul>
 *
 * <p>L'annotation {@code Order(-2)} prend le pas sur le
 * {@code DefaultErrorWebExceptionHandler} de Spring Boot, enregistre a -1.
 */
@Slf4j
@Component
@Order(-2)
public class GatewayErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final GatewayErrorAttributes errorAttributes;

    public GatewayErrorWebExceptionHandler(GatewayErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatusCode status = resolveStatus(ex);
        Map<String, Object> attrs = errorAttributes.attributesFor(
                status,
                request.getMethod(),
                request.getPath().value(),
                messageOf(ex),
                exchange.getRequest().getId());

        log.warn("{} {} -> {} ({})", request.getMethod(), request.getPath().value(),
                status.value(), attrs.get("targetService"));

        response.setStatusCode(status);
        // CORS: le frontend tourne sur une autre origine (Vite :5180) et doit
        // pouvoir lire le corps de l'erreur, sinon l'axios du navigateur ne
        // reçoit qu'un "Network Error" opaque.
        response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");

        if (wantsHtml(request)) {
            response.getHeaders().setContentType(MediaType.TEXT_HTML);
            byte[] body = renderHtml(status, attrs).getBytes(StandardCharsets.UTF_8);
            return write(response, body);
        }

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = writeJson(status, attrs);
        return write(response, body);
    }

    private static HttpStatusCode resolveStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            return rse.getStatusCode();
        }
        if (ex instanceof org.springframework.web.server.UnsupportedMediaTypeStatusException ums) {
            return ums.getStatusCode();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * HTML only when the client actually asked for HTML as a concrete type.
     *
     * <p>A plain {@code isCompatibleWith} test is wrong here: a wildcard
     * {@code * / *} is "compatible" with everything, so newman, curl's default
     * and axios would all receive an HTML page and every JSON assertion in the
     * Postman suite would fail on "Unexpected token '<'". A browser sends
     * {@code text/html, ... , * / *;q=0.8} — the concrete {@code text/html} is
     * what identifies it, and it always comes first.
     *
     * <p>So: scan for the first non-wildcard acceptable type and serve HTML only
     * if that one is HTML. axios sends {@code application/json} first and gets
     * JSON; newman sends only {@code * / *} and gets JSON; a browser gets HTML.
     */
    private static boolean wantsHtml(ServerHttpRequest request) {
        List<MediaType> accepted = request.getHeaders().getAccept();
        for (MediaType type : accepted) {
            if (type.isWildcardType()) {
                continue;
            }
            return MediaType.TEXT_HTML.isCompatibleWith(type);
        }
        return false;
    }

    private static byte[] writeJson(HttpStatusCode status, Map<String, Object> attrs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timestamp", attrs.get("timestamp"));
        payload.put("status", status.value());
        payload.put("error", status.toString());
        payload.put("method", attrs.get("method"));
        payload.put("path", attrs.get("path"));
        payload.put("targetService", attrs.get("targetService"));
        payload.put("hint", attrs.get("hint"));
        payload.put("message", attrs.get("message"));
        try {
            return MAPPER.writeValueAsBytes(payload);
        } catch (Exception e) {
            return ("{\"status\":" + status.value() + "}").getBytes(StandardCharsets.UTF_8);
        }
    }

    private static String messageOf(Throwable ex) {
        if (ex == null) {
            return "";
        }
        if (ex instanceof ResponseStatusException rse && rse.getReason() != null) {
            return rse.getReason();
        }
        if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
            return ex.getMessage();
        }
        return ex.getClass().getSimpleName();
    }

    private static Mono<Void> write(ServerHttpResponse response, byte[] body) {
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }

    // ── HTML ──────────────────────────────────────────────────────────────

    private static String renderHtml(HttpStatusCode status, Map<String, Object> attrs) {
        String statusText = status.toString();
        String title = titleFor(status);
        String css = CSS;
        StringBuilder sb = new StringBuilder(8192);

        sb.append("<!doctype html><html lang=\"fr\"><head><meta charset=\"utf-8\">")
          .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
          .append("<title>").append(status.value()).append(' ').append(esc(statusText))
          .append(" &middot; API Gateway CSPH</title><style>").append(css).append("</style></head><body>")
          .append("<main class=\"wrap\">");

        sb.append("<header class=\"hd\"><div class=\"code\">").append(status.value()).append("</div>")
          .append("<div><h1>").append(esc(title)).append("</h1>")
          .append("<p class=\"sub\">").append(esc(statusText)).append("</p></div></header>");

        sb.append("<section class=\"card\">")
          .append(row("Requête", String.valueOf(attrs.getOrDefault("method", "?")) + "  "
                  + attrs.getOrDefault("path", "?")))
          .append(row("Microservice visé", String.valueOf(attrs.getOrDefault("targetService", "—"))))
          .append(row("Message", String.valueOf(attrs.getOrDefault("message", "—"))))
          .append(row("Horodatage", String.valueOf(attrs.getOrDefault("timestamp", "—"))))
          .append("</section>");

        sb.append("<section class=\"card hint\"><h2>Ce que ça veut dire</h2><p>")
          .append(esc(String.valueOf(attrs.getOrDefault("hint", ""))))
          .append("</p></section>");

        sb.append("<section class=\"card\"><h2>Prefixes routés par la gateway</h2><div class=\"tags\">");
        Object prefixes = attrs.get("routedPrefixes");
        if (prefixes instanceof List<?> list) {
            for (Object p : list) {
                sb.append("<code>").append(esc(String.valueOf(p))).append("</code>");
            }
        }
        sb.append("</div><p class=\"foot\">Documentation interactive : ")
          .append("<a href=\"/swagger-ui.html\">/swagger-ui.html</a> &middot; ")
          .append("Application web : <a href=\"http://localhost:5180/\">http://localhost:5180/</a>")
          .append("</p></section>");

        sb.append("</main></body></html>");
        return sb.toString();
    }

    private static String titleFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "Requête invalide";
            case 401 -> "Authentification requise";
            case 403 -> "Accès refusé";
            case 404 -> "Route introuvable sur la gateway";
            case 405 -> "Méthode HTTP non autorisée";
            case 415 -> "Type de contenu non supporté";
            case 500 -> "Erreur interne du gateway";
            case 503 -> "Service indisponible (aucune instance enregistrée)";
            case 504 -> "Délai dépassé vers le microservice";
            default -> "Erreur " + status.value();
        };
    }

    private static String row(String label, String value) {
        return "<div class=\"row\"><div class=\"k\">" + esc(label) + "</div>"
                + "<div class=\"v\">" + esc(value) + "</div></div>";
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static final String CSS = """
            *,*::before,*::after{box-sizing:border-box}
            body{margin:0;background:#f6f7f9;color:#111827;
              font:15px/1.55 ui-sans-serif,system-ui,-apple-system,"Segoe UI",Roboto,sans-serif}
            .wrap{max-width:900px;margin:0 auto;padding:40px 24px 72px}
            .hd{display:flex;align-items:center;gap:20px;margin-bottom:28px}
            .code{font:700 44px/1 ui-monospace,SFMono-Regular,Menlo,monospace;color:#b91c1c;
              background:#fee2e2;border:1px solid #fecaca;border-radius:12px;padding:14px 18px}
            h1{margin:0;font-size:24px;letter-spacing:-.01em}
            .sub{margin:2px 0 0;color:#6b7280;font-size:14px}
            .card{background:#fff;border:1px solid #e5e7eb;border-radius:12px;padding:18px 20px;
              margin-bottom:16px;box-shadow:0 1px 2px rgba(0,0,0,.04)}
            .card h2{margin:0 0 12px;font-size:13px;letter-spacing:.08em;text-transform:uppercase;color:#6b7280}
            .row{display:grid;grid-template-columns:180px 1fr;gap:12px;padding:7px 0;
              border-top:1px solid #f3f4f6}
            .row:first-of-type{border-top:0}
            .k{color:#6b7280;font-size:13px}
            .v{font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:13px;word-break:break-all}
            .hint p{margin:0;color:#374151}
            .tags{display:flex;flex-wrap:wrap;gap:6px}
            .tags code{background:#f3f4f6;border:1px solid #e5e7eb;border-radius:6px;
              padding:3px 7px;font-size:12px}
            .foot{margin:14px 0 0;color:#6b7280;font-size:13px}
            a{color:#b45309}
            """;
}
