package com.survisha.meghaconnect.scheduler;

import com.survisha.meghaconnect.service.VoiceRemarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class VoiceRemarkTranscriptionScheduler {
    private final VoiceRemarkService service;

    @Scheduled(fixedDelayString = "${speech.worker-interval-ms:30000}", initialDelayString = "${speech.worker-initial-delay-ms:10000}")
    public void process() { service.processPending(); }
}
