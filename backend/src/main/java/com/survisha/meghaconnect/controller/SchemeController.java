package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.SchemeDto;
import com.survisha.meghaconnect.dto.SchemeDocumentDto;
import com.survisha.meghaconnect.service.SchemeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Schemes", description = "Scheme management and configuration (admin only)")
@SecurityRequirement(name = "bearerAuth")
@org.springframework.security.access.prepost.PreAuthorize("@accessPolicy.canManageCmoConfiguration()")
public class SchemeController {

    private final SchemeService schemeService;

    /**
     * Get all CM schemes (from reference_data)
     */
    @Operation(summary = "Get all schemes", description = "Retrieve all CM schemes from reference data")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved schemes",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = SchemeDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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
    @Operation(summary = "Get scheme by code", description = "Retrieve a specific scheme and its required documents by scheme code")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved scheme",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = SchemeDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Scheme not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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
    @Operation(summary = "Create scheme", description = "Create a new scheme in the reference data")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Scheme created successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = SchemeDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid scheme input"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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
    @Operation(summary = "Update scheme", description = "Update scheme active/inactive status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Scheme updated successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = SchemeDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid scheme input"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Scheme not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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
    @Operation(summary = "Configure scheme documents", description = "Configure required documents for a scheme")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Scheme documents configured successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = SchemeDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid document configuration"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Scheme not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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
