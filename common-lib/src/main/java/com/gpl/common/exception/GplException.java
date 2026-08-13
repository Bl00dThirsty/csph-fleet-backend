package com.gpl.common.exception;

import lombok.Getter;

/**
 * Exception de base du système GPL.
 */
@Getter
public class GplException extends RuntimeException {

    private final String errorCode;
    private final String field;

    public GplException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.field = null;
    }

    public GplException(String errorCode, String message, String field) {
        super(message);
        this.errorCode = errorCode;
        this.field = field;
    }

    public GplException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.field = null;
    }
}
