package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.OtpTemp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpTempRepository extends JpaRepository<OtpTemp, Long> {

    /** Find the latest non-consumed, non-expired OTP for a phone number. */
    Optional<OtpTemp> findTopByPhoneNumberAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String phoneNumber, LocalDateTime now);

    /** Count failed attempts within a time window to support brute-force detection. */
    @Query("SELECT COALESCE(SUM(o.attemptCount), 0) FROM OtpTemp o " +
           "WHERE o.phoneNumber = :phone AND o.createdAt > :since")
    int sumAttemptCountSince(String phone, LocalDateTime since);

    /** Purge expired records (run periodically). */
    @Modifying
    @Transactional
    @Query("DELETE FROM OtpTemp o WHERE o.expiresAt < :cutoff")
    void deleteExpiredBefore(LocalDateTime cutoff);
}
