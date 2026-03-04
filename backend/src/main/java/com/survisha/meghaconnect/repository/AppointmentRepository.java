package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {

    Optional<Appointment> findByApplicationId(String applicationId);

    List<Appointment> findByApplicant_Id(Long applicantId);

    List<Appointment> findByStatus(Appointment.AppointmentStatus status);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.applicant.id = :personId AND a.scheduledDateTime >= :sixMonthsAgo AND a.status = 'COMPLETED'")
    int countMeetingsLast6Months(Long personId, java.time.LocalDateTime sixMonthsAgo);

    @Query("SELECT a FROM Appointment a WHERE a.applicant.epicNumber = :epicNumber")
    List<Appointment> findByApplicant_EpicNumber(String epicNumber);

    @Query("SELECT a FROM Appointment a WHERE a.applicant.phoneNumber = :phoneNumber")
    List<Appointment> findByApplicant_PhoneNumber(String phoneNumber);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.createdAt >= :from")
    long countCreatedSince(java.time.LocalDateTime from);
}
