package com.survisha.meghaconnect.formextraction.service;

import com.survisha.meghaconnect.formextraction.config.FormExtractionProperties;
import com.survisha.meghaconnect.formextraction.dto.*;
import com.survisha.meghaconnect.formextraction.dto.ExtractedVisitorField.Confidence;
import com.survisha.meghaconnect.formextraction.dto.ExtractedVisitorField.FieldStatus;
import com.survisha.meghaconnect.formextraction.validation.VisitorFormImageValidator;
import com.survisha.meghaconnect.formextraction.exception.FormExtractionException;
import com.survisha.meghaconnect.formextraction.provider.FormExtractionInput;
import com.survisha.meghaconnect.formextraction.provider.FormExtractionProviderResolver;
import com.survisha.meghaconnect.formextraction.provider.VisitorFormExtractionResult;
import com.survisha.meghaconnect.service.AuditLogService;
import com.survisha.meghaconnect.util.DateTimeUtil;
import com.survisha.meghaconnect.util.RequestContextUtil;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitorFormExtractionServiceImpl implements VisitorFormExtractionService {
    private final VisitorFormImageValidator imageValidator;
    private final FormExtractionProviderResolver providerResolver;
    private final FormExtractionProperties properties;
    private final AuditLogService auditLogService;
    private final MeterRegistry meterRegistry;

    @Override
    public VisitorFormExtractionResponse extract(MultipartFile image, String formType, String languageHint, String actor) {
        if (!properties.isEnabled()) {
            throw new FormExtractionException("FORM_EXTRACTION_DISABLED", "AI form extraction is disabled.", 503);
        }
        var validated = imageValidator.validate(image);
        if (!validated.quality().isAcceptable()) {
            return qualityFailure(validated.quality());
        }
        long start = System.nanoTime();
        FormExtractionInput input = FormExtractionInput.builder().imageBytes(validated.bytes())
                .mimeType(validated.mimeType()).formType(formType).formVersion(properties.getFormVersion())
                .languageHint(languageHint == null ? properties.getLanguageHint() : languageHint)
                .requestId(RequestContextUtil.getRequestId()).build();
        VisitorFormExtractionResult result = providerResolver.resolve(properties.getProvider()).extract(input);
        ExtractedVisitorField<String> name = result.getExtractedName();
        ExtractedVisitorField<String> mobile = result.getExtractedMobileNumber();
        ExtractedVisitorField<Integer> age = result.getExtractedAge();
        ExtractedVisitorField<String> address = result.getExtractedAddress();
        validateName(name); validateMobile(mobile); validateAge(age); validateAddress(address);
        List<String> warnings = new ArrayList<>();
        warnings.addAll(result.getWarnings() == null ? List.of() : result.getWarnings());
        boolean review = result.isRequiresManualReview() || !name.isValid() || !mobile.isValid()
                || !age.isValid() || !address.isValid() || uncertain(name) || uncertain(mobile) || uncertain(age) || uncertain(address);
        int fieldsFound = (name.getValue()!=null?1:0)+(mobile.getValue()!=null?1:0)+(age.getValue()!=null?1:0)+(address.getValue()!=null?1:0);
        long durationMs = (System.nanoTime()-start)/1_000_000;
        auditLogService.log("VisitorFormExtraction", null, "EXTRACTED",
                "fieldsFound="+fieldsFound+", manualReview="+review+", durationMs="+durationMs+
                        ", provider="+result.getProvider()+", model="+result.getModel(), actor);
        String provider = result.getProvider().name().toLowerCase(Locale.ROOT);
        String model = result.getModel() == null ? "unknown" : result.getModel();
        meterRegistry.counter("form_extraction_success_total", "provider", provider, "model", model).increment();
        meterRegistry.timer("form_extraction_duration", "provider", provider, "model", model)
                .record(java.time.Duration.ofMillis(durationMs));
        if (review) meterRegistry.counter("form_extraction_manual_review_total", "provider", provider, "model", model).increment();
        log.info("Visitor form extraction completed provider={} fieldsFound={} manualReview={} durationMs={} model={}",
                provider, fieldsFound, review, durationMs, model);
        return VisitorFormExtractionResponse.builder().success(true).documentType("VISITOR_REGISTRATION")
                .formVersion(properties.getFormVersion()).name(name).mobileNumber(mobile).age(age).address(address)
                .warnings(warnings).requiresManualReview(true).imageQuality(validated.quality())
                .requestId(RequestContextUtil.getRequestId()).extractionTimestamp(DateTimeUtil.nowIST())
                .modelVersion(result.getModel())
                .message("AI-extracted values are suggestions. Review and confirm every field before registration.").build();
    }

    private VisitorFormExtractionResponse qualityFailure(ImageQualityResult quality) {
        return VisitorFormExtractionResponse.builder().success(false).documentType("VISITOR_REGISTRATION")
                .formVersion(properties.getFormVersion()).warnings(quality.getIssues()).requiresManualReview(true)
                .imageQuality(quality).requestId(RequestContextUtil.getRequestId()).extractionTimestamp(DateTimeUtil.nowIST())
                .message("The captured form image is too low quality. Please capture it again.").build();
    }
    private void validateName(ExtractedVisitorField<String> f) {
        f.setValue(normalize(f.getValue())); String v=f.getValue();
        f.setValid(v!=null && v.length()<=properties.getMaxNameLength() && v.chars().filter(Character::isDigit).count()*2 < v.length());
    }
    private void validateMobile(ExtractedVisitorField<String> f) {
        String v=f.getValue(); if(v!=null) v=v.replace(" ","").replace("-",""); f.setValue(v);
        f.setValid(v!=null && v.matches("[6-9][0-9]{9}"));
    }
    private void validateAge(ExtractedVisitorField<Integer> f) {
        Integer v=f.getValue(); f.setValid(v!=null && v>=properties.getMinAge() && v<=properties.getMaxAge());
    }
    private void validateAddress(ExtractedVisitorField<String> f) {
        f.setValue(normalize(f.getValue())); String v=f.getValue();
        f.setValid(v!=null && v.length()<=properties.getMaxAddressLength());
    }
    private String normalize(String value) { return value==null||value.isBlank()?null:value.trim().replaceAll("\\s+"," "); }
    private boolean uncertain(ExtractedVisitorField<?> f) {
        return f.getStatus()!=FieldStatus.EXTRACTED || f.getConfidence()==Confidence.LOW || f.getConfidence()==Confidence.NONE;
    }
}
