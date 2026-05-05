package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.OtpTemp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpTempRepository extends JpaRepository<OtpTemp, Long> {

    /** Find the latest non-consumed, non-expired OTP for a phone number. */
    Optional<OtpTemp> findTopByPhoneNumberAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String phoneNumber, LocalDateTime now);

    /** Find the latest non-consumed, non-expired OTP for a resolved visitor. */
    Optional<OtpTemp> findTopByPhoneNumberAndVisitorIdAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String phoneNumber, Long visitorId, LocalDateTime now);

    /** Find the latest KYC/registration-flow OTP that is not tied to a visitor yet. */
    Optional<OtpTemp> findTopByPhoneNumberAndVisitorIdIsNullAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String phoneNumber, LocalDateTime now);

    /** Count failed attempts within a time window to support brute-force detection. */
    @Query("SELECT COALESCE(SUM(o.attemptCount), 0) FROM OtpTemp o " +
           "WHERE o.phoneNumber = :phone AND o.createdAt > :since")
    int sumAttemptCountSince(@Param("phone") String phone, @Param("since") LocalDateTime since);

    /** Purge expired records (run periodically). */
    @Modifying
    @Transactional
    @Query("DELETE FROM OtpTemp o WHERE o.expiresAt < :cutoff")
    void deleteExpiredBefore(LocalDateTime cutoff);
}
