package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.AppointmentDocumentAiNotes;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentDocumentAiNotesRepository extends JpaRepository<AppointmentDocumentAiNotes, Long> {

    List<AppointmentDocumentAiNotes> findByAppointmentIdOrderByCreatedAtAsc(Long appointmentId);

    Optional<AppointmentDocumentAiNotes> findByDocumentId(Long documentId);
}
