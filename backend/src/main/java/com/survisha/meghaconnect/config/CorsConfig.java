package com.survisha.meghaconnect.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.survisha.meghaconnect.util.RequestContextUtil;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Global CORS configuration for MeghaConnect.
 * Central CORS configuration for approved MeghaConnect web/mobile origins.
 */
@Configuration
public class CorsConfig {

    @Value("${meghaconnect.cors.allowed-origins:https://meghaconnect.cloud,https://www.meghaconnect.com}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        configuration.setAllowedOriginPatterns(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toList()));
        
        // Allow all HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"));
        
        // Allow all headers (including Authorization with JWT token)
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Expose headers that the frontend can read
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", RequestContextUtil.REQUEST_ID_HEADER));
        
        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
