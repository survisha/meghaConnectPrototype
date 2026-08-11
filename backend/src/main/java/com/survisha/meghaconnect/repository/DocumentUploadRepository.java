package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.DocumentUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentUploadRepository extends JpaRepository<DocumentUpload, Long> {

    /**
     * Find all documents uploaded for a specific appointment
     */
    List<DocumentUpload> findByAppointmentId(Long appointmentId);

    /**
     * Find all documents uploaded for a specific visitor
     */
    List<DocumentUpload> findByVisitorId(Long visitorId);

    /**
     * Find a specific document by appointment and document type
     */
    Optional<DocumentUpload> findByAppointmentIdAndDocumentType(Long appointmentId, String documentType);

    /**
     * Find all documents of a specific type for a visitor
     */
    List<DocumentUpload> findByVisitorIdAndDocumentType(Long visitorId, String documentType);

    List<DocumentUpload> findByFollowUp_IdOrderByUploadedDateDesc(Long followUpId);
    List<DocumentUpload> findByAppointment_IdIn(List<Long> appointmentIds);
}
