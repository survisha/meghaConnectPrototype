package com.survisha.meghaconnect.legacy.controller;
import com.survisha.meghaconnect.legacy.dto.*;
import com.survisha.meghaconnect.legacy.service.LegacyPersonSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/legacy-data/search") @RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DEO','APPROVER','HCM')")
public class LegacyPersonSearchController {
    private final LegacyPersonSearchService service;
    @PostMapping("/person") public LegacyPersonSearchResponse person(@RequestBody LegacyPersonSearchRequest request, Authentication auth){return service.search(request,auth.getName());}
}
