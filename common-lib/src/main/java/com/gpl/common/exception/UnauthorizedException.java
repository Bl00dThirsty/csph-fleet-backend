package com.gpl.common.exception;

/**
 * Exception for 401 Unauthorized access attempts.
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   04.08.2026
 */
public class UnauthorizedException extends GplException {

    /*
     * Constructor with message.
     */
    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }
}
