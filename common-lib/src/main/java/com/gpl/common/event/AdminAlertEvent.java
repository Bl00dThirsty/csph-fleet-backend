package com.gpl.common.event;

import lombok.Data;

import java.time.Instant;

/**
 * POJO for admin alert events.
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   04.08.2026
 */
@Data
public class AdminAlertEvent {

    private String serviceName;
    private String errorLevel;
    private String message;
    private String stackTrace;
    private Instant timestamp;
}
