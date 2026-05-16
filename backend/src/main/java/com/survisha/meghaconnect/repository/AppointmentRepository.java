package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Collection;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {

    Optional<Appointment> findByApplicationId(String applicationId);

    List<Appointment> findByApplicant_Id(Long applicantId);

    List<Appointment> findByApplicant_IdOrderByCreatedAtDesc(Long applicantId);

    Optional<Appointment> findByIdAndApplicant_Id(Long id, Long applicantId);

    List<Appointment> findByStatus(Appointment.AppointmentStatus status);

    List<Appointment> findByStatusOrderByCreatedAtAsc(Appointment.AppointmentStatus status);

    List<Appointment> findByStatusIn(Collection<Appointment.AppointmentStatus> statuses);

    List<Appointment> findByScheduledDateTimeGreaterThanEqualAndScheduledDateTimeLessThanOrderByScheduledDateTimeAsc(
        java.time.LocalDateTime start,
        java.time.LocalDateTime end);

    List<Appointment> findByPublicDarbar_IdOrderByPublicDarbarTokenNumberAsc(Long publicDarbarId);

    boolean existsByPublicDarbar_IdAndStatusIn(Long publicDarbarId, Collection<Appointment.AppointmentStatus> statuses);
    
    Page<Appointment> findByStatusIn(List<Appointment.AppointmentStatus> statuses, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.applicant.id = :personId AND a.scheduledDateTime >= :sixMonthsAgo AND a.status = 'COMPLETED'")
    int countMeetingsLast6Months(@Param("personId") Long personId, @Param("sixMonthsAgo") java.time.LocalDateTime sixMonthsAgo);

    @Query("SELECT a FROM Appointment a WHERE a.applicant.epicNumber = :epicNumber")
    List<Appointment> findByApplicant_EpicNumber(@Param("epicNumber") String epicNumber);

    @Query("SELECT a FROM Appointment a WHERE a.applicant.phoneNumber = :phoneNumber")
    List<Appointment> findByApplicant_PhoneNumber(@Param("phoneNumber") String phoneNumber);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.createdAt >= :from")
    long countCreatedSince(@Param("from") java.time.LocalDateTime from);

    @Query("SELECT a FROM Appointment a JOIN FETCH a.applicant " +
        "WHERE a.scheduledDateTime IS NOT NULL " +
        "ORDER BY a.scheduledDateTime ASC")
    List<Appointment> findScheduledWithApplicant();

    @Query("SELECT a FROM Appointment a JOIN FETCH a.applicant " +
        "WHERE a.scheduledDateTime >= :start AND a.scheduledDateTime < :end " +
        "ORDER BY a.scheduledDateTime ASC")
    List<Appointment> findScheduledWithApplicantInRange(
        @Param("start") java.time.LocalDateTime start,
        @Param("end") java.time.LocalDateTime end);
}
