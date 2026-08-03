package com.survisha.meghaconnect.face.service;

import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.repository.VisitorRepository;
import com.survisha.meghaconnect.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FaceEnrollmentStatusService {
    private final VisitorRepository visitorRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(Long visitorId) {
        Visitor visitor = visitorRepository.findById(visitorId).orElseThrow();
        visitor.setFaceEnrollmentStatus("SUCCESS");
        visitor.setFaceEnrolledOn(DateTimeUtil.nowIST());
        visitor.setFaceEnrollmentMessage("Face enrolled successfully.");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long visitorId, String message) {
        Visitor visitor = visitorRepository.findById(visitorId).orElseThrow();
        visitor.setFaceEnrollmentStatus("FAILED");
        visitor.setFaceEnrolledOn(null);
        visitor.setFaceEnrollmentMessage(limit(message));
    }

    private String limit(String message) {
        String value = message == null || message.isBlank() ? "Face enrollment failed." : message.trim();
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
