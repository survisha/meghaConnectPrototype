package com.survisha.meghaconnect.util;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Centralized application clock for all business timestamps.
 */
public final class DateTimeUtil {

    public static final String IST_ZONE_ID = "Asia/Kolkata";
    public static final ZoneId IST_ZONE = ZoneId.of(IST_ZONE_ID);

    private DateTimeUtil() {
    }

    public static LocalDateTime nowIST() {
        return LocalDateTime.now(IST_ZONE);
    }

    public static LocalDate currentDateIST() {
        return LocalDate.now(IST_ZONE);
    }

    public static Timestamp currentTimestampIST() {
        return Timestamp.valueOf(nowIST());
    }
}
