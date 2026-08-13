package com.gpl.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation de sécurité RBAC. Apposée sur une méthode de controller,
 * elle vérifie que l'utilisateur courant possède la permission spécifiée.
 *
 * <p>Exemple d'utilisation :</p>
 * <pre>
 *   {@literal @}RequiresPermission("tours.create")
 *   public ResponseEntity&lt;...&gt; createTour(...) { ... }
 * </pre>
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   13.08.2026
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
    /**
     * Code de la permission requise (ex: "tours.create").
     */
    String value();
}
