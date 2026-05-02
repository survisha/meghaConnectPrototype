package com.survisha.meghaconnect.scheduler;

import com.survisha.meghaconnect.dto.PublicDarbarResponse;
import com.survisha.meghaconnect.service.PublicDarbarSchedulingService;
import com.survisha.meghaconnect.service.PublicDarbarService;
import com.survisha.meghaconnect.util.RequestContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PublicDarbarScheduler {

    private static final String SYSTEM_ACTOR = "public-darbar-scheduler";
    private static final String SYSTEM_ROLE = "SYSTEM";

    private final PublicDarbarService publicDarbarService;
    private final PublicDarbarSchedulingService schedulingService;

    @Scheduled(fixedDelayString = "${meghaconnect.public-darbar.scheduler-delay-ms:60000}")
    public void scheduleSelectedAppointments() {
        String jobId = "PD-SCHED-" + RequestContextUtil.generateRequestId();
        RequestContextUtil.setRequestId(jobId);
        try {
            List<PublicDarbarResponse> activeDarbars = publicDarbarService.findActiveDarbars();
            if (activeDarbars.isEmpty()) {
                log.debug("Public Darbar scheduler skipped because no active dates exist");
                return;
            }

            log.info("Public Darbar scheduler started jobId={} activeDarbarCount={}", jobId, activeDarbars.size());
            for (PublicDarbarResponse darbar : activeDarbars) {
                try {
                    schedulingService.scheduleSelectedForDarbar(darbar.getId(), SYSTEM_ACTOR, SYSTEM_ROLE, jobId, false);
                } catch (Exception e) {
                    log.error("Public Darbar scheduler failed for publicDarbarId={} jobId={}",
                            darbar.getId(), jobId, e);
                }
            }
            log.info("Public Darbar scheduler completed jobId={}", jobId);
        } finally {
            RequestContextUtil.clear();
        }
    }
}
