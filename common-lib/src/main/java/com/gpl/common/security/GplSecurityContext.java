package com.gpl.common.security;

import java.util.Collections;
import java.util.Set;

/**
 * Contexte de sécurité thread-local pour la requête HTTP courante.
 * Peuplé par {@link GplSecurityContextFilter} à partir des headers
 * injectés par l'API Gateway après validation du JWT.
 *
 * <p>Utilisation depuis n'importe quel point du code :</p>
 * <pre>
 *   String orgId = GplSecurityContext.getCurrentOrgId();
 *   boolean canCreate = GplSecurityContext.hasPermission("tours.create");
 * </pre>
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   13.08.2026
 */
public final class GplSecurityContext {

    private GplSecurityContext() {
        // Utility class — no instantiation
    }

    /**
     * Données de sécurité de la requête courante.
     */
    public static final class SecurityData {
        private final String personId;
        private final String username;
        private final String orgId;
        private final Set<String> roles;
        private final Set<String> permissions;

        public SecurityData(String personId, String username, String orgId,
                            Set<String> roles, Set<String> permissions) {
            this.personId = personId;
            this.username = username;
            this.orgId = orgId;
            this.roles = roles != null ? Collections.unmodifiableSet(roles) : Collections.emptySet();
            this.permissions = permissions != null ? Collections.unmodifiableSet(permissions) : Collections.emptySet();
        }

        public String getPersonId() { return personId; }
        public String getUsername() { return username; }
        public String getOrgId() { return orgId; }
        public Set<String> getRoles() { return roles; }
        public Set<String> getPermissions() { return permissions; }
    }

    private static final ThreadLocal<SecurityData> CONTEXT = new ThreadLocal<>();

    // ──────────── Setters (used by GplSecurityContextFilter) ────────────

    /**
     * Initialise le contexte de sécurité pour la requête courante.
     * Appelé exclusivement par {@link GplSecurityContextFilter}.
     */
    public static void set(SecurityData data) {
        CONTEXT.set(data);
    }

    /**
     * Nettoie le contexte de sécurité. DOIT être appelé dans un bloc finally
     * pour éviter les fuites de mémoire dans les pools de threads.
     */
    public static void clear() {
        CONTEXT.remove();
    }

    // ──────────── Getters (used by application code) ────────────

    /**
     * @return Les données de sécurité complètes, ou null si aucun contexte n'est défini
     *         (appel interne entre services par exemple).
     */
    public static SecurityData get() {
        return CONTEXT.get();
    }

    /**
     * @return L'identifiant de la personne connectée, ou null.
     */
    public static String getCurrentPersonId() {
        SecurityData data = CONTEXT.get();
        return data != null ? data.getPersonId() : null;
    }

    /**
     * @return Le username de la personne connectée, ou null.
     */
    public static String getCurrentUsername() {
        SecurityData data = CONTEXT.get();
        return data != null ? data.getUsername() : null;
    }

    /**
     * @return L'identifiant de l'organisation de la personne connectée, ou null.
     */
    public static String getCurrentOrgId() {
        SecurityData data = CONTEXT.get();
        return data != null ? data.getOrgId() : null;
    }

    /**
     * @return Les rôles de la personne connectée (ensemble immuable), ou ensemble vide.
     */
    public static Set<String> getCurrentRoles() {
        SecurityData data = CONTEXT.get();
        return data != null ? data.getRoles() : Collections.emptySet();
    }

    /**
     * @return Les permissions de la personne connectée (ensemble immuable), ou ensemble vide.
     */
    public static Set<String> getCurrentPermissions() {
        SecurityData data = CONTEXT.get();
        return data != null ? data.getPermissions() : Collections.emptySet();
    }

    /**
     * Vérifie si l'utilisateur courant possède la permission spécifiée.
     *
     * @param permission Code de la permission à vérifier
     * @return true si la permission est présente, false sinon
     */
    public static boolean hasPermission(String permission) {
        return getCurrentPermissions().contains(permission);
    }

    /**
     * Vérifie si l'utilisateur courant possède AU MOINS UNE des permissions spécifiées.
     *
     * @param permissions Codes des permissions à vérifier (logique OR)
     * @return true si au moins une permission est présente
     */
    public static boolean hasAnyPermission(String... permissions) {
        Set<String> current = getCurrentPermissions();
        for (String p : permissions) {
            if (current.contains(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true si un contexte de sécurité est défini pour la requête courante
     *         (i.e., la requête est passée par le Gateway avec un JWT valide).
     */
    public static boolean isAuthenticated() {
        SecurityData data = CONTEXT.get();
        return data != null && data.getPersonId() != null && !data.getPersonId().isEmpty();
    }
}
