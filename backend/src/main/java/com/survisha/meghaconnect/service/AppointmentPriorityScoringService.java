package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.Appointment;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

@Service
public class AppointmentPriorityScoringService {

    public PriorityScore score(Appointment appointment) {
        int score = 25;
        List<String> reasons = new ArrayList<>();

        String text = (safe(appointment.getAgendaType()) + " " + safe(appointment.getAgendaBrief()) + " "
                + safe(appointment.getSubject()) + " " + safe(appointment.getReasonForAppointment()))
                .toLowerCase(Locale.ROOT);

        if (containsAny(text, "medical", "health", "hospital", "emergency", "urgent", "life", "death")) {
            score += 35;
            reasons.add("Health, emergency, or life-impacting keywords were found.");
        }
        if (containsAny(text, "public grievance", "law and order", "violence", "disaster", "flood", "landslide")) {
            score += 25;
            reasons.add("Public grievance, safety, or disaster-related keywords were found.");
        }
        if (containsAny(text, "road", "water", "electricity", "school", "bridge", "infrastructure")) {
            score += 15;
            reasons.add("Infrastructure or essential public service keywords were found.");
        }
        if (containsAny(text, "scheme", "benefit", "pension", "scholarship", "financial assistance")) {
            score += 10;
            reasons.add("Scheme or benefit-related keywords were found.");
        }

        Integer repeatMeetings = appointment.getMeetingCountLast6Months();
        if (repeatMeetings != null && repeatMeetings >= 3) {
            score -= 20;
            reasons.add("Applicant has multiple recent meetings, reducing priority unless urgent.");
        } else if (repeatMeetings != null && repeatMeetings == 0) {
            score += 5;
            reasons.add("No recent completed meetings were recorded.");
        }

        if (appointment.getCreatedAt() != null) {
            long ageDays = Duration.between(appointment.getCreatedAt(), LocalDateTime.now()).toDays();
            if (ageDays >= 14) {
                score += 15;
                reasons.add("Request has been pending for at least two weeks.");
            } else if (ageDays >= 7) {
                score += 8;
                reasons.add("Request has been pending for at least one week.");
            }
        }

        score = Math.max(0, Math.min(100, score));
        String level = score >= 70 ? "HIGH" : score >= 40 ? "MEDIUM" : "LOW";
        if (reasons.isEmpty()) {
            reasons.add("No high-risk keywords or repeat-meeting signals were detected.");
        }

        return PriorityScore.builder()
                .level(level)
                .score(score)
                .reasons(reasons)
                .recommendation(switch (level) {
                    case "HIGH" -> "Prioritize for early review.";
                    case "MEDIUM" -> "Schedule through normal review queue.";
                    default -> "Keep in standard queue unless staff override is justified.";
                })
                .build();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriorityScore {
        private String level;
        private int score;
        private List<String> reasons;
        private String recommendation;
    }
}
