package com.survisha.meghaconnect.config;

import com.survisha.meghaconnect.util.DateTimeUtil;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class JacksonTimeZoneConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer istJacksonCustomizer() {
        return builder -> builder.timeZone(TimeZone.getTimeZone(DateTimeUtil.IST_ZONE_ID));
    }
}
