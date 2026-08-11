package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.CreateUserRequest;
import com.survisha.meghaconnect.dto.UserResponse;
import com.survisha.meghaconnect.entity.User;
import com.survisha.meghaconnect.entity.Department;
import com.survisha.meghaconnect.exception.MeghaConnectException;
import com.survisha.meghaconnect.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.survisha.meghaconnect.repository.DepartmentAccessRequestRepository;
import com.survisha.meghaconnect.repository.DepartmentRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private DepartmentAccessRequestRepository departmentAccessRequestRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private UserService userService;

    @Test
    void departmentAdminCannotUnlockAnotherDepartmentsUser() {
        Department own = Department.builder().id(1L).departmentCode("HEALTH").status(Department.DepartmentStatus.ACTIVE).build();
        Department other = Department.builder().id(2L).departmentCode("EDU").status(Department.DepartmentStatus.ACTIVE).build();
        User actor = User.builder().id(10L).username("health.admin").role(User.UserRole.DEPARTMENT_ADMIN).department(own).build();
        User target = User.builder().id(20L).username("edu.deo").role(User.UserRole.DEO).department(other).locked(true).build();
        when(userRepository.findByNormalizedUsername("health.admin")).thenReturn(java.util.Optional.of(actor));
        when(userRepository.findById(20L)).thenReturn(java.util.Optional.of(target));
        MeghaConnectException ex = assertThrows(MeghaConnectException.class,
                () -> userService.unlockUser(20L, "health.admin"));
        assertEquals(403, ex.getHttpStatus());
        verify(userRepository, never()).save(target);
    }

    @Test
    void departmentAdminGetsOnlyOwnDepartmentPage() {
        Department own = Department.builder().id(1L).departmentCode("HEALTH")
                .departmentName("Health").status(Department.DepartmentStatus.ACTIVE).build();
        User actor = User.builder().id(10L).username("health.admin")
                .role(User.UserRole.DEPARTMENT_ADMIN).department(own).build();
        User deo = User.builder().id(20L).username("health.deo").fullName("Health DEO")
                .role(User.UserRole.DEO).department(own).active(true).build();
        var pageable = PageRequest.of(0, 10);
        when(userRepository.findByNormalizedUsername("health.admin")).thenReturn(java.util.Optional.of(actor));
        when(userRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(deo), pageable, 1));

        var page = userService.getUserResponsesForActor(
                "health.admin", null, null, null, null, null, pageable);

        assertEquals(1, page.getTotalElements());
        assertEquals("health.deo", page.getContent().get(0).getUsername());
        assertEquals(1L, page.getContent().get(0).getDepartmentId());
    }

    @Test
    void departmentAdminDepartmentTamperingIsRejected() {
        Department own = Department.builder().id(1L).departmentCode("HEALTH")
                .status(Department.DepartmentStatus.ACTIVE).build();
        User actor = User.builder().username("health.admin")
                .role(User.UserRole.DEPARTMENT_ADMIN).department(own).build();
        when(userRepository.findByNormalizedUsername("health.admin")).thenReturn(java.util.Optional.of(actor));

        MeghaConnectException ex = assertThrows(MeghaConnectException.class, () ->
                userService.getUserResponsesForActor("health.admin", null, null, null,
                        null, 2L, PageRequest.of(0, 10)));

        assertEquals(403, ex.getHttpStatus());
        verify(userRepository, never()).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void departmentAdminWithoutActiveDepartmentIsRejected() {
        User missing = User.builder().username("missing.admin")
                .role(User.UserRole.DEPARTMENT_ADMIN).build();
        when(userRepository.findByNormalizedUsername("missing.admin")).thenReturn(java.util.Optional.of(missing));
        assertEquals(403, assertThrows(MeghaConnectException.class, () ->
                userService.getUserResponsesForActor("missing.admin", null, null, null,
                        null, null, PageRequest.of(0, 10))).getHttpStatus());

        Department inactive = Department.builder().id(3L)
                .status(Department.DepartmentStatus.INACTIVE).build();
        User inactiveActor = User.builder().username("inactive.admin")
                .role(User.UserRole.DEPARTMENT_ADMIN).department(inactive).build();
        when(userRepository.findByNormalizedUsername("inactive.admin")).thenReturn(java.util.Optional.of(inactiveActor));
        assertEquals(403, assertThrows(MeghaConnectException.class, () ->
                userService.getUserResponsesForActor("inactive.admin", null, null, null,
                        null, null, PageRequest.of(0, 10))).getHttpStatus());
    }

    @Test
    void superAdminCanRequestUnscopedPage() {
        User actor = User.builder().username("superadmin").role(User.UserRole.SUPER_ADMIN).build();
        var pageable = PageRequest.of(0, 5);
        when(userRepository.findByNormalizedUsername("superadmin")).thenReturn(java.util.Optional.of(actor));
        when(userRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));
        assertTrue(userService.getUserResponsesForActor(
                "superadmin", null, null, null, null, null, pageable).isEmpty());
    }

    @Test
    void createUserEncryptsPasswordAndSavesActiveUser() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newuser")
                .fullName("New User")
                .password("secret1")
                .role(User.UserRole.DEO)
                .phoneNumber("9876543210")
                .build();

        when(roleService.existsByRoleName("DEO")).thenReturn(true);
        when(passwordEncoder.encode("secret1")).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });

        UserResponse response = userService.createUser(request, "admin");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertEquals(42L, response.getId());
        assertEquals("$2a$10$encoded", saved.getPasswordHash());
        assertNotEquals("secret1", saved.getPasswordHash());
        assertTrue(saved.isActive());
        assertEquals("admin", saved.getCreatedBy());
        assertEquals("9876543210", saved.getPhoneNumber());
    }

    @Test
    void createUserRejectsDuplicateUsername() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("admin")
                .fullName("Admin")
                .password("secret1")
                .role(User.UserRole.ADMIN)
                .build();

        when(userRepository.existsByNormalizedUsername("admin")).thenReturn(true);

        MeghaConnectException ex = assertThrows(MeghaConnectException.class,
                () -> userService.createUser(request, "admin"));

        assertEquals(409, ex.getHttpStatus());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUserRejectsRoleMissingFromRolesTable() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("security1")
                .fullName("Security")
                .password("secret1")
                .role(User.UserRole.SECURITY)
                .build();

        when(roleService.existsByRoleName("SECURITY")).thenReturn(false);

        MeghaConnectException ex = assertThrows(MeghaConnectException.class,
                () -> userService.createUser(request, "admin"));

        assertEquals(400, ex.getHttpStatus());
        verify(userRepository, never()).save(any(User.class));
    }
}
