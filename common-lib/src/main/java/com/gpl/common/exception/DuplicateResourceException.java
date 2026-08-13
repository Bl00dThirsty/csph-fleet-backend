package com.gpl.common.exception;

/**
 * Exception for 409 Conflict when a unique constraint is violated.
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   04.08.2026
 */
public class DuplicateResourceException extends GplException {

    /*
     * Constructor with message.
     */
    public DuplicateResourceException(String message) {
        super("DUPLICATE", message);
    }
}
