package com.gpl.common.constant;

import lombok.experimental.UtilityClass;

/**
 * Constants for logging.
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   04.08.2026
 */
@UtilityClass
public class LoggingConstants {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String USER_ID_HEADER = "X-User-Username";
    public static final String ORG_ID_HEADER = "X-User-OrgId";
}
