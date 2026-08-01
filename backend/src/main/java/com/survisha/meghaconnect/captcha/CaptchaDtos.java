package com.survisha.meghaconnect.captcha;

import javax.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

public final class CaptchaDtos {
    private CaptchaDtos() { }

    @Schema(description = "Captcha generation response.")
    public record CaptchaResponse(
            @Schema(description = "Opaque captcha ID.", example = "2e8428a2-f3dd-41e5-9959-3f91d2f5de35") String captchaId,
            @Schema(description = "Base64 PNG captcha image. Use this in production clients.") String captchaImage,
            @Schema(description = "Dev/testing only fallback answer text. Do not expose this in production when image captcha is enabled.", example = "A7KQ9") String captchaText,
            @Schema(description = "Captcha expiry timestamp in UTC.", example = "2026-06-23T10:25:30Z") String expiresAt) { }

    @Schema(description = "Captcha validation request.")
    public record CaptchaValidateRequest(
            @Schema(description = "Captcha ID returned by generate API.", example = "2e8428a2-f3dd-41e5-9959-3f91d2f5de35")
            @NotBlank(message = "Captcha ID is required") String captchaId,
            @Schema(description = "Captcha answer entered by the user.", example = "A7KQ9")
            @NotBlank(message = "Captcha is required") String captchaValue) { }

    @Schema(description = "Captcha validation response.")
    public record CaptchaValidateResponse(
            @Schema(description = "Whether captcha validation succeeded.", example = "true") boolean valid,
            @Schema(description = "Validation message.", example = "Captcha validated successfully") String message) { }
}
