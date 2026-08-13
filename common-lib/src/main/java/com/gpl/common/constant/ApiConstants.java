package com.gpl.common.constant;

import lombok.experimental.UtilityClass;

/**
 * Constants for API.
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   04.08.2026
 */
@UtilityClass
public class ApiConstants {

    public static final String API_PREFIX = "/api/v1";
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_FIELD = "createdAt";
    public static final String DEFAULT_SORT_DIRECTION = "desc";
}
