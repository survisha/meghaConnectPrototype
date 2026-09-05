package com.survisha.meghaconnect.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component @RequiredArgsConstructor
public class VoiceRemarkQueueListener {
    private final VoiceTranscriptionQueue queue;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterStored(VoiceRemarkStoredEvent event) { queue.submit(event.getVoiceRemarkId()); }
}
