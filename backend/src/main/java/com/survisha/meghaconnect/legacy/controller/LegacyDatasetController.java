package com.survisha.meghaconnect.legacy.controller;
import com.survisha.meghaconnect.legacy.dto.LegacyImportDtos.*;
import com.survisha.meghaconnect.legacy.service.LegacyImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/legacy-datasets") @RequiredArgsConstructor
public class LegacyDatasetController {
    private final LegacyImportService service;
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','DEO')") public List<DatasetInfo> list(){return service.datasets();}
    @PostMapping @PreAuthorize("hasRole('ADMIN')") public DatasetInfo create(@RequestBody DatasetRequest request, Authentication auth){return service.createDataset(request,auth.getName());}
}
