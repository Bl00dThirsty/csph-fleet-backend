package com.gpl.common.exception;

/**
 * Exception for 422 Unprocessable Entity when business rules are violated.
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   04.08.2026
 */
public class BusinessException extends GplException {

    /*
     * Constructor with message.
     */
    public BusinessException(String message) {
        super("BUSINESS_RULE_VIOLATION", message);
    }
}
