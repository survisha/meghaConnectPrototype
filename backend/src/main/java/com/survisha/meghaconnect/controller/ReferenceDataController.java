package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.ReferenceDataDto;
import com.survisha.meghaconnect.service.ReferenceDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reference")
@RequiredArgsConstructor
@Tag(name = "Reference Data", description = "API for reference data management")
public class ReferenceDataController {

    private final ReferenceDataService referenceDataService;

    @GetMapping("/{type}")
    @Operation(summary = "Get reference data by type", description = "Retrieve dropdown values for a specific reference type")
    public ResponseEntity<List<ReferenceDataDto>> getReferenceData(
            @PathVariable String type,
            @RequestParam(required = false) String parentCode) {
        List<ReferenceDataDto> data = referenceDataService.getReferenceDataByType(type.toUpperCase(), parentCode);
        return ResponseEntity.ok(data);
    }
}
