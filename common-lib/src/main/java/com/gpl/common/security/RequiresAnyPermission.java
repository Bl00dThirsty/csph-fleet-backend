package com.gpl.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation de sécurité RBAC (logique OR). Apposée sur une méthode de controller,
 * elle vérifie que l'utilisateur courant possède AU MOINS UNE des permissions spécifiées.
 *
 * <p>Exemple d'utilisation :</p>
 * <pre>
 *   {@literal @}RequiresAnyPermission({"tours.read", "tours.manage"})
 *   public ResponseEntity&lt;...&gt; listTours(...) { ... }
 * </pre>
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   13.08.2026
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresAnyPermission {
    /**
     * Codes des permissions (logique OR). L'accès est accordé si l'utilisateur
     * possède au moins une de ces permissions.
     */
    String[] value();
}
