package com.gpl.common;

import com.gpl.common.enums.TourExecutionMode;
import com.gpl.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("common-lib test harness")
class CommonLibSmokeTest {

    @Test
    @DisplayName("the surefire provider resolves and executes assertions")
    void surefireRuns() {
        assertEquals(2, TourExecutionMode.values().length,
                "TourExecutionMode doit avoir exactement INTERNAL et EXTERNAL");
    }

    @Test
    @DisplayName("BusinessException is constructible from the common-lib test classpath")
    void businessExceptionIsAvailable() {
        assertThrows(BusinessException.class, () -> {
            throw new BusinessException("test");
        });
    }
}
