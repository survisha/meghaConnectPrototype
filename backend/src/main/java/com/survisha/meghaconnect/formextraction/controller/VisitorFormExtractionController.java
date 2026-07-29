package com.survisha.meghaconnect.formextraction.controller;

import com.survisha.meghaconnect.formextraction.dto.VisitorFormExtractionResponse;
import com.survisha.meghaconnect.formextraction.service.VisitorFormExtractionService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/visitor-form-extraction")
@RequiredArgsConstructor
public class VisitorFormExtractionController {
    private final VisitorFormExtractionService service;

    @PostMapping(value="/extract", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DEPARTMENT_ADMIN','DEPARTMENT_PA','DATA_ENTRY_OPERATOR')")
    @Operation(summary="Extract handwritten visitor details for operator review",
            description="Returns AI suggestions only. It never registers or persists visitor details or the image.")
    public VisitorFormExtractionResponse extract(@RequestPart("image") MultipartFile image,
                                                 @RequestParam(defaultValue="VISITOR_REGISTRATION") String formType,
                                                 @RequestParam(required=false) String languageHint,
                                                 Authentication authentication) {
        return service.extract(image, formType, languageHint, authentication.getName());
    }
}
