package com.survisha.meghaconnect.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AppointmentWalkInQueryRegressionTest {

    private AppointmentRepository appointments;
    private RequestValidationService validation;
    private UserRepository users;
    private AppointmentService service;

    @BeforeEach
    void setUp() {
        appointments = mock(AppointmentRepository.class);
        validation = mock(RequestValidationService.class);
        users = mock(UserRepository.class);
        service = new AppointmentService(
            appointments,
            mock(AssociateMappingRepository.class),
            mock(VisitorRepository.class),
            mock(DocumentUploadRepository.class),
            mock(ScheduleEventRepository.class),
            mock(SchemeApplicationRepository.class),
            mock(AuditLogService.class),
            validation,
            mock(FileStorageService.class),
            mock(AppointmentDocumentAiNotesService.class),
            mock(QrTokenService.class),
            mock(WalkInRepository.class),
            mock(WalkInTokenService.class),
            users,
            new ObjectMapper(),
            mock(AppointmentLifecycleService.class),
            mock(AppointmentAuditService.class));
        when(users.findByNormalizedUsername(anyString())).thenReturn(Optional.empty());
        when(appointments.findWalkInsByDateAndStatusIn(any(), anyCollection(), any()))
            .thenReturn(Page.empty());
        when(validation.requireEnum(anyString(), eq(Appointment.AppointmentStatus.class), eq("status")))
            .thenAnswer(invocation -> Appointment.AppointmentStatus.valueOf(invocation.getArgument(0)));
    }

    @Test
    void bindsSelectedStatusesAsVarcharNamesAndPreservesPageRequest() {
        LocalDate date = LocalDate.of(2026, 9, 2);

        service.findAllDtosForActor("admin", "PENDING,COMPLETED", null,
            "B2 Walk-in", null, date, PageRequest.of(1, 10));

        verify(appointments).findWalkInsByDateAndStatusIn(
            eq(date), eq(List.of("PENDING", "COMPLETED")), eq(PageRequest.of(1, 10)));
    }

    @Test
    void noStatusFilterStillBindsOnlyKnownStringStatusNames() {
        service.findAllDtosForActor("admin", null, null,
            "B2 Walk-in", null, LocalDate.of(2026, 9, 2), PageRequest.of(0, 10));

        @SuppressWarnings("unchecked")
        var statuses = (Collection<String>) mockingDetails(appointments).getInvocations().stream()
            .filter(invocation -> invocation.getMethod().getName().equals("findWalkInsByDateAndStatusIn"))
            .findFirst().orElseThrow().getArgument(1);
        assertEquals(Appointment.AppointmentStatus.values().length, statuses.size());
        assertTrue(statuses.containsAll(List.of("PENDING", "COMPLETED")));
        assertFalse(statuses.contains("DUMMY"));
        assertTrue(statuses.stream().allMatch(String.class::isInstance));
    }

    @Test
    void nativeDataAndCountQueriesExcludeDummyBeforeHydration() throws Exception {
        assertSqlGuard("findWalkInsByDateAndStatusIn", LocalDate.class, Collection.class,
            org.springframework.data.domain.Pageable.class);
        assertSqlGuard("findWalkInsByDateAndStatusInAndDepartment", LocalDate.class,
            Collection.class, Long.class, org.springframework.data.domain.Pageable.class);
        assertFalse(List.of(Appointment.AppointmentStatus.values()).stream()
            .anyMatch(status -> status.name().equals("DUMMY")));
    }

    private void assertSqlGuard(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = AppointmentRepository.class.getMethod(methodName, parameterTypes);
        Query query = method.getAnnotation(Query.class);
        assertNotNull(query);
        assertTrue(query.nativeQuery());
        assertTrue(query.value().contains("a.status <> 'DUMMY'"));
        assertTrue(query.countQuery().contains("a.status <> 'DUMMY'"));
        assertTrue(query.value().contains("a.status IN (:statuses)"));
        assertTrue(query.countQuery().contains("a.status IN (:statuses)"));
    }
}
