package com.survisha.meghaconnect.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchScheduleResult {

    private String jobId;
    private Long publicDarbarId;
    private int selectedCount;
    private int scheduledCount;
    private int skippedCount;
    private int qrGeneratedCount;
    private List<Long> scheduledAppointmentIds;
    private Map<Long, String> qrTokensByAppointmentId;
}
