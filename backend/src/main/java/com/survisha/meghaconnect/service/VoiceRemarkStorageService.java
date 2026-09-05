package com.survisha.meghaconnect.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

@Service
public class VoiceRemarkStorageService {
    private static final Map<String, String> SIGNATURES = Map.of(
            "wav", "RIFF", "m4a", "ftyp", "aac", "ADTS", "mp3", "ID3", "ogg", "OggS", "webm", "webm");
    private static final Set<String> MIME_TYPES = Set.of("audio/mp4", "audio/m4a", "audio/x-m4a", "audio/aac",
            "audio/wav", "audio/x-wav", "audio/mpeg", "audio/ogg", "audio/webm", "video/mp4", "application/octet-stream");

    @Value("${voice.storage-path:${meghaconnect.storage.base-path:/uploads}/voice-remarks}") private String storagePath;
    @Value("${voice.max-file-size-mb:12}") private long maxFileSizeMb;
    @Value("${voice.max-duration-ms:60000}") private long maxDurationMs;

    public StoredAudio store(MultipartFile audio, Long durationMs) {
        validate(audio, durationMs);
        String ext = extension(audio.getOriginalFilename());
        String name = "voice_" + UUID.randomUUID() + "." + ext;
        Path root = Paths.get(storagePath).toAbsolutePath().normalize();
        Path target = root.resolve(name).normalize();
        if (!target.startsWith(root)) throw new IllegalArgumentException("Invalid audio storage path.");
        try {
            Files.createDirectories(root);
            try (InputStream in = audio.getInputStream()) {
                Files.copy(in, target);
            }
            return new StoredAudio(name, ext, target.toString(), audio.getSize(), safeOriginalName(audio.getOriginalFilename()));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to store voice recording safely.", e);
        }
    }

    public Path resolve(String storedPath) {
        Path root = Paths.get(storagePath).toAbsolutePath().normalize();
        Path value = Paths.get(storedPath).toAbsolutePath().normalize();
        if (!value.startsWith(root) || !Files.isRegularFile(value) || !Files.isReadable(value)) {
            throw new IllegalArgumentException("Voice recording is unavailable.");
        }
        return value;
    }

    private void validate(MultipartFile file, Long durationMs) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("A non-empty audio recording is required.");
        if (file.getSize() > maxFileSizeMb * 1024 * 1024) throw new IllegalArgumentException("Voice recording exceeds the size limit.");
        if (durationMs != null && (durationMs <= 0 || durationMs > maxDurationMs)) throw new IllegalArgumentException("Voice recording duration is invalid.");
        String ext = extension(file.getOriginalFilename());
        if (!SIGNATURES.containsKey(ext)) throw new IllegalArgumentException("Unsupported audio format.");
        String mime = Optional.ofNullable(file.getContentType()).orElse("application/octet-stream").toLowerCase(Locale.ROOT);
        if (!MIME_TYPES.contains(mime)) throw new IllegalArgumentException("Unsupported audio MIME type.");
        byte[] header = new byte[16];
        int count;
        try (InputStream in = file.getInputStream()) { count = in.read(header); }
        catch (IOException e) { throw new IllegalArgumentException("Unable to inspect audio recording.", e); }
        if (count < 4 || !matchesSignature(ext, header)) throw new IllegalArgumentException("Audio content does not match its format.");
    }

    private boolean matchesSignature(String ext, byte[] b) {
        if ("wav".equals(ext)) return ascii(b, 0, "RIFF") && ascii(b, 8, "WAVE");
        if ("m4a".equals(ext)) return ascii(b, 4, "ftyp");
        if ("aac".equals(ext)) return (b[0] & 0xff) == 0xff && ((b[1] & 0xf6) == 0xf0);
        if ("mp3".equals(ext)) return ascii(b, 0, "ID3") || ((b[0] & 0xff) == 0xff && (b[1] & 0xe0) == 0xe0);
        if ("ogg".equals(ext)) return ascii(b, 0, "OggS");
        if ("webm".equals(ext)) return (b[0] & 0xff) == 0x1a && (b[1] & 0xff) == 0x45 && (b[2] & 0xff) == 0xdf && (b[3] & 0xff) == 0xa3;
        return false;
    }
    private boolean ascii(byte[] b, int offset, String value) {
        if (b.length < offset + value.length()) return false;
        for (int i = 0; i < value.length(); i++) if (b[offset + i] != (byte) value.charAt(i)) return false;
        return true;
    }
    private String extension(String name) {
        String safe = Optional.ofNullable(name).orElse("").toLowerCase(Locale.ROOT);
        int dot = safe.lastIndexOf('.'); return dot < 0 ? "" : safe.substring(dot + 1);
    }
    private String safeOriginalName(String name) {
        String value = Optional.ofNullable(name).orElse("recording").replace('\\', '/');
        value = value.substring(value.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._-]", "_");
        return value.substring(0, Math.min(255, value.length()));
    }
    @lombok.Value public static class StoredAudio { String fileName; String format; String absolutePath; long size; String originalName; }
}
