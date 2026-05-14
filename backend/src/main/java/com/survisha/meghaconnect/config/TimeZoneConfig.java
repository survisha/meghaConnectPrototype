package com.survisha.meghaconnect.config;

import com.survisha.meghaconnect.util.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

@Configuration
@Slf4j
public class TimeZoneConfig {

    @PostConstruct
    public void setApplicationTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(DateTimeUtil.IST_ZONE_ID));
        log.info("Application timezone set to IST (Asia/Kolkata)");
    }
}
