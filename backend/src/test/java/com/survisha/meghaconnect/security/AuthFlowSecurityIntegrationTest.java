package com.survisha.meghaconnect.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.survisha.meghaconnect.config.CorsConfig;
import com.survisha.meghaconnect.config.SecurityConfig;
import com.survisha.meghaconnect.controller.AppointmentController;
import com.survisha.meghaconnect.controller.AuthController;
import com.survisha.meghaconnect.dto.AuthResponse;
import com.survisha.meghaconnect.entity.Department;
import com.survisha.meghaconnect.entity.User;
import com.survisha.meghaconnect.repository.UserRepository;
import com.survisha.meghaconnect.repository.VisitorRepository;
import com.survisha.meghaconnect.service.AppointmentDocumentAiNotesService;
import com.survisha.meghaconnect.service.AppointmentService;
import com.survisha.meghaconnect.service.AuditLogService;
import com.survisha.meghaconnect.service.AuthService;
import com.survisha.meghaconnect.service.HcmActionService;
import com.survisha.meghaconnect.service.ScheduleEventService;
import com.survisha.meghaconnect.service.VisitorPassService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, AppointmentController.class})
@Import({
    SecurityConfig.class,
    CorsConfig.class,
    JwtAuthenticationFilter.class,
    JwtService.class,
    ApiRateLimitFilter.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class
})
@TestPropertySource(properties = {
    "app.jwt.secret=CHANGE_ME_MIN_256_BIT_SECRET_FOR_UAT_ONLY",
    "app.jwt.expiration-ms=86400000"
})
class AuthFlowSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;
    @MockBean
    private VisitorRepository visitorRepository;
    @MockBean
    private AuthService authService;
    @MockBean
    private AppointmentService appointmentService;
    @MockBean
    private AppointmentDocumentAiNotesService appointmentDocumentAiNotesService;
    @MockBean
    private ScheduleEventService scheduleEventService;
    @MockBean
    private HcmActionService hcmActionService;
    @MockBean
    private VisitorPassService visitorPassService;
    @MockBean
    private AuditLogService auditLogService;

    @Test
    void loginTokenCanCallProtectedAppointmentsEndpoint() throws Exception {
        User admin = user("admin", User.UserRole.ADMIN, activeDepartment());
        String token = tokenFor(admin);
        when(authService.login(any())).thenReturn(AuthResponse.builder()
            .accessToken(token)
            .token(token)
            .tokenType("Bearer")
            .username("admin")
            .role("ROLE_ADMIN")
            .departmentId(admin.getDepartment().getId())
            .build());
        when(userRepository.findByNormalizedUsername("admin")).thenReturn(Optional.of(admin));
        when(appointmentService.findAllDtosForActor(eq("admin"), isNull(), isNull(), isNull(), any(Pageable.class)))
            .thenReturn(Page.empty());

        String loginBody = mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(loginBody);
        String accessToken = json.get("accessToken").asText();

        mockMvc.perform(get("/api/v1/appointments?page=0&size=1000")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk());
    }

    @Test
    void missingAuthorizationHeaderReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/appointments?page=0&size=1000"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedAuthorizationTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/appointments?page=0&size=1000")
                .header("Authorization", "Bearer not-a-jwt"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenWithWrongRoleReturns403() throws Exception {
        User security = user("security", User.UserRole.SECURITY, activeDepartment());
        String token = tokenFor(security);
        when(userRepository.findByNormalizedUsername("security")).thenReturn(Optional.of(security));

        mockMvc.perform(get("/api/v1/appointments?page=0&size=1000")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    void superAdminTokenWorksWithNullDepartment() throws Exception {
        User superAdmin = user("superadmin", User.UserRole.SUPER_ADMIN, null);
        String token = tokenFor(superAdmin);
        when(userRepository.findByNormalizedUsername("superadmin")).thenReturn(Optional.of(superAdmin));
        when(appointmentService.findAllDtosForActor(eq("superadmin"), isNull(), isNull(), isNull(), any(Pageable.class)))
            .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/appointments?page=0&size=1000")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void adminTokenWorksWithActiveDefaultDepartment() throws Exception {
        User admin = user("admin", User.UserRole.ADMIN, activeDepartment());
        String token = tokenFor(admin);
        when(userRepository.findByNormalizedUsername("admin")).thenReturn(Optional.of(admin));
        when(appointmentService.findAllDtosForActor(eq("admin"), isNull(), isNull(), isNull(), any(Pageable.class)))
            .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/appointments?page=0&size=1000")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    private String tokenFor(User user) {
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPasswordHash(),
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        return jwtService.generateToken(userDetails, user);
    }

    private User user(String username, User.UserRole role, Department department) {
        User user = User.builder()
            .username(username)
            .fullName(username)
            .passwordHash("$2a$10$encoded")
            .role(role)
            .department(department)
            .active(true)
            .locked(false)
            .build();
        user.setId(1L);
        return user;
    }

    private Department activeDepartment() {
        Department department = new Department();
        department.setId(10L);
        department.setDepartmentCode("CMO");
        department.setDepartmentName("Chief Minister's Office");
        department.setStatus(Department.DepartmentStatus.ACTIVE);
        return department;
    }
}
