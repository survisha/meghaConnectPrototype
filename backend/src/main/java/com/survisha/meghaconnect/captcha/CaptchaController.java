package com.survisha.meghaconnect.captcha;


import javax.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.survisha.meghaconnect.captcha.CaptchaDtos.CaptchaResponse;
import com.survisha.meghaconnect.captcha.CaptchaDtos.CaptchaValidateRequest;
import com.survisha.meghaconnect.captcha.CaptchaDtos.CaptchaValidateResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/captcha")
@Tag(name = "Captcha APIs", description = "Public captcha generation and validation APIs for login.")
@SecurityRequirements
public class CaptchaController {
    private final CaptchaService service;

    public CaptchaController(CaptchaService service) {
        this.service = service;
    }

    @GetMapping("/generate")
    @Operation(summary = "Generate captcha", description = "Generates a single-use captcha valid for 5 minutes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Captcha generated."),
            @ApiResponse(responseCode = "500", description = "Internal server error.")
    })
    public CaptchaResponse generate() {
        return service.generate();
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate captcha", description = "Validates and consumes a captcha. Captchas are single-use.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Captcha validated."),
            @ApiResponse(responseCode = "400", description = "Captcha invalid or expired."),
            @ApiResponse(responseCode = "500", description = "Internal server error.")
    })
    public CaptchaValidateResponse validate(@Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Captcha validation request.") @RequestBody CaptchaValidateRequest request) {
        return service.validate(request.captchaId(), request.captchaValue());
    }
}
