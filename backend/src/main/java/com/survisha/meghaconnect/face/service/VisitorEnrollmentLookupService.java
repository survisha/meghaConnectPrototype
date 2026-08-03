package com.survisha.meghaconnect.face.service;

import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.repository.VisitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VisitorEnrollmentLookupService {
    private static final String VISITOR_PREFIX = "VISITOR_";
    private final VisitorRepository visitorRepository;

    @Transactional(readOnly = true)
    public Optional<Visitor> findVisitorByEnrollmentId(String enrollmentId) {
        if (enrollmentId == null || enrollmentId.isBlank()) return Optional.empty();
        String value = enrollmentId.trim();
        if (value.matches("VISITOR_[1-9][0-9]*")) {
            return visitorRepository.findById(Long.parseLong(value.substring(VISITOR_PREFIX.length())));
        }
        return visitorRepository.findByEpicNumberIgnoreCase(value);
    }
}
