package com.survisha.meghaconnect.face.integration;

import com.survisha.meghaconnect.face.dto.FaceRequests;
import com.survisha.meghaconnect.face.dto.FaceResponses;
import com.survisha.meghaconnect.face.service.FaceRecognitionService;
import com.survisha.meghaconnect.service.AuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Destructive manual UAT test. It is excluded from normal Surefire discovery by
 * its IT suffix and also requires an explicit environment flag.
 */
@Slf4j
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "FACE_DELETE_MANUAL_ENABLED", matches = "(?i)true")
class FaceRecognitionDeleteManualIT {
    private static final Pattern SAFE_ENROLLMENT_ID = Pattern.compile("[A-Za-z0-9_-]{1,100}");
    private static final Set<String> PRODUCTION_PROFILES = Set.of("prod", "production");

    private final FaceRecognitionService faceRecognitionService;
    private final AuditLogService auditLogService;
    private final Environment environment;

    @Value("${face-delete-manual.enabled:false}") private boolean enabled;
    @Value("${face-delete-manual.enrollment-id:VISITOR_576}") private String enrollmentId;
    @Value("${face-delete-manual.actor:uat-maintenance}") private String actor;
    @Value("${face-delete-manual.allow-production:false}") private boolean allowProduction;

    @Autowired
    FaceRecognitionDeleteManualIT(FaceRecognitionService faceRecognitionService,
                                  AuditLogService auditLogService,
                                  Environment environment) {
        this.faceRecognitionService = faceRecognitionService;
        this.auditLogService = auditLogService;
        this.environment = environment;
    }

    @Test
    void deleteExplicitlyConfiguredEnrollmentFromProvider() {
        Set<String> profiles = Arrays.stream(environment.getActiveProfiles())
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(enabled, "FACE_DELETE_MANUAL_ENABLED must be true");
        assertTrue(profiles.contains("face-delete-manual"),
                "The face-delete-manual Spring profile must be active");
        assertTrue(profiles.contains("uat") || profiles.contains("face-delete-manual"),
                "Deletion is allowed only in UAT or the explicit maintenance profile");
        assertTrue(PRODUCTION_PROFILES.stream().noneMatch(profiles::contains) || allowProduction,
                "Production deletion is blocked unless FACE_DELETE_ALLOW_PRODUCTION=true");
        assertTrue(enrollmentId != null && SAFE_ENROLLMENT_ID.matcher(enrollmentId.trim()).matches(),
                "FACE_DELETE_ENROLLMENT_ID must contain only letters, numbers, underscore, or hyphen");

        String safeEnrollmentId = enrollmentId.trim();
        log.warn("Manual face deletion authorized enrollmentId={} actor={} profiles={}",
                safeEnrollmentId, actor, profiles);
        auditLogService.log("FaceEnrollment", null, "MANUAL_DELETE_ATTEMPT",
                "UAT provider deletion requested for enrollmentId=" + safeEnrollmentId, actor);

        FaceRequests.Delete request = new FaceRequests.Delete();
        request.setEnrollmentId(safeEnrollmentId);
        FaceResponses.Delete response = faceRecognitionService.delete(request);

        assertTrue(response.isSuccess(), "Provider did not confirm face deletion");
        auditLogService.log("FaceEnrollment", null, "MANUAL_DELETE_SUCCESS",
                "UAT provider deletion completed for enrollmentId=" + safeEnrollmentId, actor);
        log.warn("Manual face deletion completed enrollmentId={} success=true", safeEnrollmentId);
    }
}
