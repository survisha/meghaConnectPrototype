package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.HcmAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HcmActionRepository extends JpaRepository<HcmAction, Long> {
    
    /**
     * Find all actions for a specific appointment
     */
    List<HcmAction> findByAppointmentIdOrderByCreatedAtDesc(Long appointmentId);

    List<HcmAction> findByAppointmentIdAndActionTypeOrderByCreatedAtDesc(Long appointmentId, String actionType);

    Optional<HcmAction> findFirstByAppointmentIdAndActionTypeAndCreatedByOrderByCreatedAtDesc(Long appointmentId, String actionType, String createdBy);
    
    /**
     * Find the most recent action for an appointment
     */
    Optional<HcmAction> findFirstByAppointmentIdOrderByCreatedAtDesc(Long appointmentId);
    
    /**
     * Find all pending actions (status = PENDING)
     */
    @Query("SELECT h FROM HcmAction h WHERE h.actionStatus = 'PENDING' ORDER BY h.createdAt DESC")
    List<HcmAction> findAllPendingActions();
    
    /**
     * Find pending actions created after a specific date
     */
    @Query("SELECT h FROM HcmAction h WHERE h.actionStatus = 'PENDING' AND h.createdAt >= :since ORDER BY h.createdAt DESC")
    List<HcmAction> findRecentPendingActions(@Param("since") LocalDateTime since);
    
    /**
     * Find actions by type
     */
    @Query("SELECT h FROM HcmAction h WHERE h.actionType = :actionType ORDER BY h.createdAt DESC")
    List<HcmAction> findByActionType(@Param("actionType") String actionType);
    
    /**
     * Find pending rejections/clarification requests
     */
    @Query("SELECT h FROM HcmAction h WHERE h.actionType = 'REJECT' AND h.actionStatus = 'PENDING' ORDER BY h.createdAt DESC")
    List<HcmAction> findPendingRejections();
    
    /**
     * Find pending snoozes
     */
    @Query("SELECT h FROM HcmAction h WHERE h.actionType = 'SNOOZE' AND h.actionStatus = 'PENDING' ORDER BY h.createdAt DESC")
    List<HcmAction> findPendingSnoozedActions();
    
    /**
     * Count pending actions
     */
    @Query("SELECT COUNT(h) FROM HcmAction h WHERE h.actionStatus = 'PENDING'")
    Long countPendingActions();
    
    /**
     * Find actions for an appointment that are not completed
     */
    @Query("SELECT h FROM HcmAction h WHERE h.appointmentId = :appointmentId AND h.actionStatus != 'COMPLETED' ORDER BY h.createdAt DESC")
    List<HcmAction> findActiveActionsByAppointment(@Param("appointmentId") Long appointmentId);
}
