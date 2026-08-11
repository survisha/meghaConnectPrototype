package com.survisha.meghaconnect.formextraction.controller;

import com.survisha.meghaconnect.formextraction.dto.VisitorFormExtractionResponse;
import com.survisha.meghaconnect.formextraction.service.VisitorFormExtractionService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.survisha.meghaconnect.util.RequestContextUtil;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/visitor-form-extraction")
@RequiredArgsConstructor
@Slf4j
public class VisitorFormExtractionController {
    private final VisitorFormExtractionService service;

    @PostMapping(value="/extract", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DEPARTMENT_ADMIN','DEPARTMENT_PA','DEO','APPROVER','HCM')")
    @Operation(summary="Extract handwritten visitor details for operator review",
            description="Returns AI suggestions only. It never registers or persists visitor details or the image.")
    public VisitorFormExtractionResponse extract(@RequestPart("image") MultipartFile image,
                                                 @RequestParam(defaultValue="VISITOR_REGISTRATION") String formType,
                                                 @RequestParam(required=false) String languageHint,
                                                 Authentication authentication) {
        long started=System.nanoTime(); String requestId=RequestContextUtil.getRequestId();
        log.info("Visitor form extraction controller entered requestId={} imageSizeBytes={}",requestId,image==null?0:image.getSize());
        try {
            VisitorFormExtractionResponse response=service.extract(image, formType, languageHint, authentication.getName());
            log.info("Visitor form extraction controller completed requestId={} elapsedMs={} httpOutcome=200 success={}",
                    requestId,(System.nanoTime()-started)/1_000_000,response.isSuccess());
            return response;
        } catch(RuntimeException ex) {
            log.warn("Visitor form extraction controller failed requestId={} elapsedMs={} httpOutcome=exception exception={}",
                    requestId,(System.nanoTime()-started)/1_000_000,ex.getClass().getSimpleName());
            throw ex;
        }
    }
}
