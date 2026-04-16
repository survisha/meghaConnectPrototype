package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.service.VisitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/visitors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Visitors", description = "Visitor search and management endpoints")
public class VisitorController {

    private final VisitorService visitorService;

    @Operation(summary = "Find visitor by phone", description = "Search visitor by phone number")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Visitor found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Visitor.class))),
        @ApiResponse(responseCode = "404", description = "Visitor not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/search/phone/{phone}")
    public ResponseEntity<Visitor> findByPhone(@PathVariable String phone) {
        return visitorService.findByPhone(phone)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Find visitor by EPIC number", description = "Search visitor by EPIC number")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Visitor found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Visitor.class))),
        @ApiResponse(responseCode = "404", description = "Visitor not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/search/epic/{epic}")
    public ResponseEntity<Visitor> findByEpic(@PathVariable String epic) {
        return visitorService.findByEpic(epic)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Search visitors by name", description = "Search visitors by name using query string")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search results returned",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Visitor.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/search/name")
    public ResponseEntity<List<Visitor>> searchByName(@RequestParam String q) {
        return ResponseEntity.ok(visitorService.searchByName(q));
    }

    @Operation(summary = "Find visitors by district", description = "Retrieve all visitors from a specific district")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Visitors retrieved",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Visitor.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/search/district/{district}")
    public ResponseEntity<List<Visitor>> findByDistrict(@PathVariable String district) {
        return ResponseEntity.ok(visitorService.findByDistrict(district));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Visitor> getById(@PathVariable Long id) {
        return visitorService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Visitor> create(@RequestBody Visitor visitor) {
        return ResponseEntity.ok(visitorService.save(visitor));
    }
}
