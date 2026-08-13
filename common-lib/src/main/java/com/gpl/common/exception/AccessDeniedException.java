package com.gpl.common.exception;

import lombok.Getter;

/**
 * Exception levée lorsqu'un utilisateur tente d'accéder à une ressource
 * sans posséder la permission requise. Interceptée par le
 * {@link GlobalExceptionHandler} et traduite en HTTP 403 Forbidden.
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   13.08.2026
 */
@Getter
public class AccessDeniedException extends RuntimeException {

    private final String permissionRequired;

    public AccessDeniedException(String permissionRequired, String message) {
        super(message);
        this.permissionRequired = permissionRequired;
    }

    public AccessDeniedException(String permissionRequired) {
        super(String.format("Permission '%s' requise pour accéder à cette ressource", permissionRequired));
        this.permissionRequired = permissionRequired;
    }
}
