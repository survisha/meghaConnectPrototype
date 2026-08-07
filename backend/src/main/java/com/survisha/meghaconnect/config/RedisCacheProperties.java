package com.survisha.meghaconnect.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "cache.redis")
public class RedisCacheProperties {
    private boolean enabled;

    @NotBlank
    private String environment = "local";

    @NotNull
    private Duration otpTtl = Duration.ofMinutes(5);
    @NotNull
    private Duration captchaTtl = Duration.ofMinutes(5);
    @NotNull
    private Duration userTtl = Duration.ofMinutes(10);
    @NotNull
    private Duration citizenProfileTtl = Duration.ofMinutes(5);
    @NotNull
    private Duration schemeTtl = Duration.ofMinutes(30);
    @NotNull
    private Duration agendaTypeTtl = Duration.ofHours(1);
    @NotNull
    private Duration departmentTtl = Duration.ofHours(1);
    @NotNull
    private Duration districtTtl = Duration.ofHours(24);
    @NotNull
    private Duration mandalTtl = Duration.ofHours(24);
    @NotNull
    private Duration referenceDataTtl = Duration.ofHours(1);

    private boolean securityStoreFallbackEnabled;

    @AssertTrue(message = "all Redis TTL values must be positive")
    public boolean areTtlsPositive() {
        return positive(otpTtl) && positive(captchaTtl) && positive(userTtl)
                && positive(citizenProfileTtl) && positive(schemeTtl)
                && positive(agendaTypeTtl) && positive(departmentTtl)
                && positive(districtTtl) && positive(mandalTtl)
                && positive(referenceDataTtl);
    }

    private boolean positive(Duration value) {
        return value != null && !value.isZero() && !value.isNegative();
    }
}
