package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.entity.Grievance;
import com.survisha.meghaconnect.entity.Grievance.GrievanceStatus;
import com.survisha.meghaconnect.service.GrievanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/grievances")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GrievanceController {

    private final GrievanceService grievanceService;

    @GetMapping
    public ResponseEntity<Page<Grievance>> getAll(Pageable pageable) {
        return ResponseEntity.ok(grievanceService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Grievance> getById(@PathVariable Long id) {
        return grievanceService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Grievance create(@RequestBody Grievance grievance,
                            @AuthenticationPrincipal UserDetails user) {
        String actor = user != null ? user.getUsername() : "anonymous";
        return grievanceService.create(grievance, actor);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails user) {
        String statusStr = body.get("status");
        if (statusStr == null || statusStr.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "'status' field is required");
            return ResponseEntity.badRequest().body(error);
        }
        try {
            GrievanceStatus status = GrievanceStatus.valueOf(statusStr);
            String actor = user != null ? user.getUsername() : "anonymous";
            return ResponseEntity.ok(
                    grievanceService.updateStatus(id, status, body.get("remarks"), actor)
            );
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Invalid status: " + statusStr);
            return ResponseEntity.badRequest().body(error);
        }
    }
}
