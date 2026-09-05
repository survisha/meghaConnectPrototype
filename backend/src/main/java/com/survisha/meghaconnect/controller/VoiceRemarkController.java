package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.VoiceRemarkResponse;
import com.survisha.meghaconnect.entity.VoiceRemark;
import com.survisha.meghaconnect.service.VoiceRemarkService;
import com.survisha.meghaconnect.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/v1/voice-remarks")
@PreAuthorize("hasAnyRole('APPROVER','HCM')")
@RequiredArgsConstructor
public class VoiceRemarkController {
    private final VoiceRemarkService service;
    private final AuditLogService auditLogService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VoiceRemarkResponse> upload(@RequestPart("audio") MultipartFile audio,
            @RequestParam String referenceType, @RequestParam String referenceId,
            @RequestParam String requestId, @RequestParam(required = false) Long durationMs, Authentication auth) {
        String role = auth.getAuthorities().stream().map(Object::toString)
                .filter(a -> a.equals("ROLE_APPROVER") || a.equals("ROLE_HCM")).findFirst().orElseThrow();
        VoiceRemarkResponse response = service.upload(audio, referenceType, referenceId,
                requestId, durationMs, auth.getName(), role.substring(5));
        auditLogService.log("VoiceRemark", response.getVoiceRemarkId(), "VOICE_AUDIO_STORED",
                "Voice audio stored for " + response.getReferenceType() + " " + response.getReferenceId(), auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}") public VoiceRemarkResponse get(@PathVariable Long id) { return service.get(id); }

    @GetMapping
    public List<VoiceRemarkResponse> list(@RequestParam String referenceType, @RequestParam String referenceId) {
        return service.list(referenceType, referenceId);
    }

    @PostMapping("/{id}/retry-transcription") public VoiceRemarkResponse retry(@PathVariable Long id, Authentication auth) {
        VoiceRemarkResponse response = service.retry(id);
        auditLogService.log("VoiceRemark", id, "VOICE_TRANSCRIPTION_RETRY", "Voice transcription queued for retry", auth.getName());
        return response;
    }

    @GetMapping("/{id}/audio")
    public ResponseEntity<FileSystemResource> audio(@PathVariable Long id, Authentication auth) {
        VoiceRemark meta = service.audioMetadata(id); Path path = service.audioPath(id);
        auditLogService.log("VoiceRemark", id, "VOICE_AUDIO_PLAYBACK", "Authorized voice audio access", auth.getName());
        MediaType type;
        try { type = MediaType.parseMediaType(formatMime(meta.getAudioFormat())); }
        catch (Exception ignored) { type = MediaType.APPLICATION_OCTET_STREAM; }
        return ResponseEntity.ok().contentType(type).contentLength(meta.getAudioSizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + meta.getAudioFileName() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store").body(new FileSystemResource(path));
    }
    private String formatMime(String format) {
        if ("m4a".equals(format)) return "audio/mp4";
        if ("mp3".equals(format)) return "audio/mpeg";
        return "audio/" + format;
    }
}
