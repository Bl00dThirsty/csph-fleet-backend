package com.gpl.common.exception;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Auto-configuration Spring Boot qui enregistre le {@link GlobalExceptionHandler}
 * dans chaque microservice.
 *
 * <p>Le handler lui-même s'annonçait avec {@code @RestControllerAdvice}, mais
 * l'annotation n'est scrutinyée que dans les paquets balayés par le composant
 * {@code @SpringBootApplication} de chaque service. Les services.scannent
 * {@code com.gpl.<service>}, jamais {@code com.gpl.common}, donc le handler
 * n'était enregistré nulle part : la totalité des erreurs de domaine
 * (introuvable, doublon, accès refusé, erreur métier, validation) remontait en
 * <b>500</b> au lieu de 404 / 409 / 403 / 422 / 400.</p>
 *
 * <p>Ce bug n'était pas visible tant que le peuplement des permissions était
 * cassé : personne n'atteignait un {@code @RequiresPermission} en échec, donc
 * personne n'observait un 403 manquant. Il est également à l'origine de
 * {@code POST /api/v1/organizations} avec un code déjà existant qui renvoyait
 * 500 au lieu de 409.</p>
 *
 * <p>Ne s'active que dans les applications Servlet (WebMvc), pas dans
 * l'API Gateway réactif (WebFlux).</p>
 *
 * @author GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since 27.09.2026
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
public class GplExceptionHandlingAutoConfiguration {

    /**
     * Enregistre le gestionnaire global. Un service peut définir le sien
     * (plus haut dans la hiérarchie) pour l'étendre ou le remplacer.
     */
    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
