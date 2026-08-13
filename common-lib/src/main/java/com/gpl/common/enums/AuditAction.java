package com.gpl.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Actions d'audit — uniquement les modifications (pas les consultations).
 */
@Getter
@RequiredArgsConstructor
public enum AuditAction {

    CREATE("CRT", "Création"),
    UPDATE("UPD", "Modification"),
    DELETE("DEL", "Suppression"),
    STATUS_CHANGE("STC", "Changement de statut"),
    ACTIVATE("ACT", "Activation"),
    DEACTIVATE("DAC", "Désactivation"),
    ASSIGN_ROLE("ASR", "Attribution de rôle"),
    REVOKE_ROLE("RVR", "Révocation de rôle"),
    ASSIGN_SITE("ASS", "Attribution de site"),
    REVOKE_SITE("RVS", "Révocation de site"),
    ADD_TO_GROUP("ATG", "Ajout au groupe"),
    REMOVE_FROM_GROUP("RFG", "Retrait du groupe"),
    LOGIN("LGN", "Connexion"),
    LOGIN_FAILED("LGF", "Échec de connexion"),
    LOGOUT("LGO", "Déconnexion"),
    PASSWORD_CHANGE("PWC", "Changement de mot de passe"),
    PASSWORD_RESET("PWR", "Réinitialisation de mot de passe");

    private final String code;
    private final String description;

    public static AuditAction fromCode(String code) {
        for (AuditAction action : values()) {
            if (action.code.equalsIgnoreCase(code)) return action;
        }
        throw new IllegalArgumentException("Unknown AuditAction code: " + code);
    }
}
