package com.survisha.meghaconnect.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class VoiceRemarkStorageServiceTest {
    @TempDir Path temp;

    @Test
    void storesM4aUnderGeneratedNameAndResolvesOnlyPrivateRoot() {
        VoiceRemarkStorageService service = configured();
        byte[] m4a = new byte[] {0, 0, 0, 16, 'f', 't', 'y', 'p', 'M', '4', 'A', ' '};
        MockMultipartFile upload = new MockMultipartFile("audio", "../../official name.m4a", "audio/mp4", m4a);

        VoiceRemarkStorageService.StoredAudio stored = service.store(upload, 20_000L);

        assertThat(stored.getFileName()).startsWith("voice_").endsWith(".m4a").doesNotContain("official");
        assertThat(service.resolve(stored.getAbsolutePath())).exists();
        assertThatThrownBy(() -> service.resolve(temp.getParent().resolve("outside.m4a").toString()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsExtensionContentMismatchAndOverlongDuration() {
        VoiceRemarkStorageService service = configured();
        MockMultipartFile fake = new MockMultipartFile("audio", "voice.m4a", "audio/mp4", "not audio".getBytes());
        assertThatThrownBy(() -> service.store(fake, 20_000L)).isInstanceOf(IllegalArgumentException.class);

        byte[] wav = new byte[] {'R','I','F','F',0,0,0,0,'W','A','V','E'};
        MockMultipartFile real = new MockMultipartFile("audio", "voice.wav", "audio/wav", wav);
        assertThatThrownBy(() -> service.store(real, 60_001L)).isInstanceOf(IllegalArgumentException.class);
    }

    private VoiceRemarkStorageService configured() {
        VoiceRemarkStorageService service = new VoiceRemarkStorageService();
        ReflectionTestUtils.setField(service, "storagePath", temp.toString());
        ReflectionTestUtils.setField(service, "maxFileSizeMb", 12L);
        ReflectionTestUtils.setField(service, "maxDurationMs", 60_000L);
        return service;
    }
}
