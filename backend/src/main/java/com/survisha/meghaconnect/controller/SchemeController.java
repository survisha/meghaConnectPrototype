package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.SchemeDto;
import com.survisha.meghaconnect.dto.SchemeDocumentDto;
import com.survisha.meghaconnect.service.SchemeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/admin/schemes")
@RequiredArgsConstructor
@Slf4j
public class SchemeController {

    private final SchemeService schemeService;

    /**
     * Get all CM schemes (from reference_data)
     */
    @GetMapping
    public ResponseEntity<?> getAllSchemes(HttpServletRequest request) {
        try {
            if (!schemeService.isAdminUser(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. Admin role required.");
            }

            List<SchemeDto> schemes = schemeService.getAllSchemes();
            return ResponseEntity.ok(schemes);
        } catch (Exception e) {
            log.error("Error fetching schemes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching schemes");
        }
    }

    /**
     * Get scheme by code with its required documents
     */
    @GetMapping("/{schemeCode}")
    public ResponseEntity<?> getSchemeByCode(@PathVariable String schemeCode, HttpServletRequest request) {
        try {
            if (!schemeService.isAdminUser(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. Admin role required.");
            }

            Optional<SchemeDto> scheme = schemeService.getSchemeByCode(schemeCode);
            if (scheme.isPresent()) {
                return ResponseEntity.ok(scheme.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Scheme not found");
            }
        } catch (Exception e) {
            log.error("Error fetching scheme", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching scheme");
        }
    }

    /**
     * Create new scheme in reference data
     */
    @PostMapping
    public ResponseEntity<?> createScheme(@RequestBody SchemeDto schemeDto, HttpServletRequest request) {
        try {
            if (!schemeService.isAdminUser(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. Admin role required.");
            }

            SchemeDto createdScheme = schemeService.createScheme(schemeDto, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdScheme);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid scheme input: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error creating scheme", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error creating scheme");
        }
    }

    /**
     * Update scheme active/inactive status
     */
    @PutMapping("/{schemeCode}")
    public ResponseEntity<?> updateScheme(@PathVariable String schemeCode, @RequestBody SchemeDto schemeDto, 
                                         HttpServletRequest request) {
        try {
            if (!schemeService.isAdminUser(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. Admin role required.");
            }

            SchemeDto updatedScheme = schemeService.updateScheme(schemeCode, schemeDto, request);
            return ResponseEntity.ok(updatedScheme);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid scheme: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating scheme", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating scheme");
        }
    }

    /**
     * Configure required documents for a scheme
     */
    @PutMapping("/{schemeCode}/documents")
    public ResponseEntity<?> configureSchemeDocuments(@PathVariable String schemeCode, 
                                                     @RequestBody List<SchemeDocumentDto> documentDtos,
                                                     HttpServletRequest request) {
        try {
            if (!schemeService.isAdminUser(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. Admin role required.");
            }

            SchemeDto updatedScheme = schemeService.configureSchemeDocuments(schemeCode, documentDtos, request);
            return ResponseEntity.ok(updatedScheme);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid scheme: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error configuring scheme documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error configuring scheme documents");
        }
    }
}
