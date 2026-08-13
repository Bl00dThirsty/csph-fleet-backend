package com.gpl.common.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Auto-configuration Spring Boot pour l'infrastructure de sécurité GPL.
 *
 * <p>S'active uniquement dans les applications Servlet (WebMvc), pas dans
 * les applications réactives (WebFlux) comme l'API Gateway.</p>
 *
 * <p>Enregistre automatiquement :</p>
 * <ul>
 *   <li>{@link GplSecurityContextFilter} — peuple le ThreadLocal depuis les headers</li>
 *   <li>{@link PermissionAspect} — intercepte les annotations @RequiresPermission</li>
 * </ul>
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   13.08.2026
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class GplSecurityAutoConfiguration {

    /**
     * Enregistre le filtre de contexte de sécurité avec une priorité haute
     * (s'exécute en premier, avant les controllers).
     */
    @Bean
    public FilterRegistrationBean<GplSecurityContextFilter> gplSecurityContextFilter() {
        FilterRegistrationBean<GplSecurityContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new GplSecurityContextFilter());
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setName("gplSecurityContextFilter");
        return registration;
    }

    /**
     * Enregistre l'aspect AOP de vérification des permissions.
     */
    @Bean
    public PermissionAspect permissionAspect() {
        return new PermissionAspect();
    }
}
