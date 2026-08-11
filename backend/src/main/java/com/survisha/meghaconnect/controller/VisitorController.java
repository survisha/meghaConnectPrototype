package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.VisitorDto;
import com.survisha.meghaconnect.dto.AssociateVisitorDto;
import com.survisha.meghaconnect.dto.PublicRegistrationDto;
import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.service.VisitorService;
import com.survisha.meghaconnect.service.VisitorAuthService;
import com.survisha.meghaconnect.util.RequestContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/visitors")
@RequiredArgsConstructor
@Tag(name = "Visitors", description = "Visitor search and management endpoints")
@Slf4j
public class VisitorController {

    private final VisitorService visitorService;
    private final VisitorAuthService visitorAuthService;

    @PostMapping("/staff-register")
    @PreAuthorize("hasAnyRole('DEO','APPROVER','HCM')")
    public ResponseEntity<Map<String, Object>> registerByStaff(
            @RequestBody PublicRegistrationDto dto,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(visitorAuthService.registerByStaff(dto, user.getUsername()));
    }

    /*
     * Backward compatibility:
     * These /api/v1/visitors/** endpoints are retained for existing search and
     * management integrations. New public visitor authentication and
     * registration APIs should be added under VisitorAuthController
     * (/api/v1/visitor/auth/**).
     */

    @Operation(summary = "Search visitors", description = "Search visitor by mobile, EPIC, or visitor reference ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Visitor found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = VisitorDto.class))),
        @ApiResponse(responseCode = "404", description = "Visitor not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','APPROVER','DEO','HCM')")
    public ResponseEntity<List<VisitorDto>> search(
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) String epic,
            @RequestParam(required = false) String referenceId) {
        logEndpoint("/api/v1/visitors/search");
        return ResponseEntity.ok(visitorService.searchDtos(mobile, epic, referenceId));
    }

    @Operation(summary = "Find visitors by phone", description = "Search visitors by phone number")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search results returned",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = VisitorDto.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/search/phone/{phone}")
    public ResponseEntity<List<VisitorDto>> findByPhone(@PathVariable String phone) {
        logEndpoint("/api/v1/visitors/search/phone/{phone}");
        return ResponseEntity.ok(visitorService.findAllByPhoneDtos(phone));
    }

    @Operation(summary = "Find visitor by EPIC number", description = "Search visitor by EPIC number")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Visitor found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = VisitorDto.class))),
        @ApiResponse(responseCode = "404", description = "Visitor not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/search/epic/{epic}")
    public ResponseEntity<VisitorDto> findByEpic(@PathVariable String epic) {
        logEndpoint("/api/v1/visitors/search/epic/{epic}");
        return visitorService.findByEpicDto(epic)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Search visitors by name", description = "Search visitors by name using query string")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search results returned",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = VisitorDto.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/search/name")
    public ResponseEntity<List<VisitorDto>> searchByName(@RequestParam String q) {
        logEndpoint("/api/v1/visitors/search/name");
        return ResponseEntity.ok(visitorService.searchByNameDtos(q));
    }

    @Operation(summary = "Find visitors by district", description = "Retrieve all visitors from a specific district")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Visitors retrieved",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = VisitorDto.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/search/district/{district}")
    public ResponseEntity<List<VisitorDto>> findByDistrict(@PathVariable String district) {
        logEndpoint("/api/v1/visitors/search/district/{district}");
        return ResponseEntity.ok(visitorService.findByDistrictDtos(district));
    }

    @GetMapping("/associate-search")
    @PreAuthorize("hasAnyRole('PUBLIC','SUPER_ADMIN','HCM','ADMIN','APPROVER','DEO')")
    public ResponseEntity<List<AssociateVisitorDto>> searchAssociateCitizens(@RequestParam String query,
                                                                             @AuthenticationPrincipal UserDetails user) {
        logEndpoint("/api/v1/visitors/associate-search");
        String actor = user != null ? user.getUsername() : "associate-search";
        return ResponseEntity.ok(visitorService.searchAssociateCitizens(query, actor));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VisitorDto> getById(@PathVariable Long id) {
        logEndpoint("/api/v1/visitors/{id}");
        return visitorService.findByIdDto(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<VisitorDto> create(@RequestBody Visitor visitor) {
        logEndpoint("/api/v1/visitors");
        return ResponseEntity.ok(visitorService.saveDto(visitor));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','APPROVER','DEO','HCM')")
    public ResponseEntity<VisitorDto> update(@PathVariable Long id,
                                             @RequestBody VisitorDto dto,
                                             @AuthenticationPrincipal UserDetails user) {
        logEndpoint("/api/v1/visitors/{id}");
        String actor = user != null ? user.getUsername() : "visitor-update";
        return ResponseEntity.ok(visitorService.updateVisitor(id, dto, actor));
    }

    private void logEndpoint(String endpoint) {
        log.info("Visitor API request requestId={} endpoint={}", RequestContextUtil.getRequestId(), endpoint);
    }
}
