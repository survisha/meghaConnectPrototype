package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.AppointmentQrToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;

@Repository
public interface AppointmentQrTokenRepository extends JpaRepository<AppointmentQrToken, Long> {

    Optional<AppointmentQrToken> findByTokenHash(String tokenHash);

    Optional<AppointmentQrToken> findTopByAppointment_IdOrderByGeneratedAtDesc(Long appointmentId);

    Optional<AppointmentQrToken> findTopByAppointment_IdAndStatusInOrderByGeneratedAtDesc(
            Long appointmentId,
            Collection<AppointmentQrToken.QrStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM AppointmentQrToken q JOIN FETCH q.appointment a JOIN FETCH q.visitor v WHERE q.tokenHash = :tokenHash")
    Optional<AppointmentQrToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
