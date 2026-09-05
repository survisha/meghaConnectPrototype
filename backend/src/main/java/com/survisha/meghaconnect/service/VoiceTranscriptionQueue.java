package com.survisha.meghaconnect.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Service @Slf4j
public class VoiceTranscriptionQueue {
    private final Executor executor;
    private final VoiceTranscriptionWorker worker;

    public VoiceTranscriptionQueue(@Qualifier("speechTaskExecutor") Executor executor, VoiceTranscriptionWorker worker) {
        this.executor = executor; this.worker = worker;
    }

    public boolean submit(Long voiceRemarkId) {
        try {
            executor.execute(() -> worker.process(voiceRemarkId));
            return true;
        } catch (RejectedExecutionException exception) {
            log.info("Speech executor is full; voiceRemarkId={} remains DB-backed PENDING", voiceRemarkId);
            return false;
        }
    }
}
