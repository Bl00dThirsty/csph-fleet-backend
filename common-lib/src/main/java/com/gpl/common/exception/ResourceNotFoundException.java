package com.gpl.common.exception;

/**
 * Exception levée quand une ressource n'est pas trouvée.
 */
public class ResourceNotFoundException extends GplException {

    public ResourceNotFoundException(String entityType, String id) {
        super("NOT_FOUND", entityType + " non trouvé(e) avec l'id: " + id);
    }

    public ResourceNotFoundException(String entityType, String field, String value) {
        super("NOT_FOUND", entityType + " non trouvé(e) avec " + field + ": " + value, field);
    }
}
