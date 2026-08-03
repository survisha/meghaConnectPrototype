package com.survisha.meghaconnect.face.event;

public record VisitorRegisteredForFaceEnrollmentEvent(
        Long visitorId,
        String epicNumber,
        String fullName,
        String photo,
        Double latitude,
        Double longitude) {
}
