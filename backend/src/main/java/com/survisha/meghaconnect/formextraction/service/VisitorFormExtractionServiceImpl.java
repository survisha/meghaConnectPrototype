package com.survisha.meghaconnect.formextraction.service;

import com.survisha.meghaconnect.formextraction.config.FormExtractionProperties;
import com.survisha.meghaconnect.formextraction.dto.*;
import com.survisha.meghaconnect.formextraction.dto.ExtractedVisitorField.Confidence;
import com.survisha.meghaconnect.formextraction.dto.ExtractedVisitorField.FieldStatus;
import com.survisha.meghaconnect.formextraction.validation.VisitorFormImageValidator;
import com.survisha.meghaconnect.formextraction.validation.VisitorFormImagePreprocessor;
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
    private final VisitorFormImagePreprocessor imagePreprocessor;
    private final FormExtractionProviderResolver providerResolver;
    private final FormExtractionProperties properties;
    private final AuditLogService auditLogService;
    private final MeterRegistry meterRegistry;

    @Override
    public VisitorFormExtractionResponse extract(MultipartFile image, String formType, String languageHint, String actor) {
        long lifecycleStart = System.nanoTime();
        String requestId = RequestContextUtil.getRequestId();
        if (!properties.isEnabled()) {
            throw new FormExtractionException("FORM_EXTRACTION_DISABLED", "AI form extraction is disabled.", 503);
        }
        var validated = imageValidator.validate(image);
        if (!validated.quality().isAcceptable()) {
            return qualityFailure(validated.quality());
        }
        var processed = imagePreprocessor.process(validated);
        log.debug("Visitor form extraction started requestId={} provider={} formType={} imageBytes={} imageWidth={} imageHeight={}",
                requestId, properties.getProvider(), formType, processed.bytes().length,
                processed.width(), processed.height());
        long start = System.nanoTime();
        FormExtractionInput input = FormExtractionInput.builder().imageBytes(processed.bytes())
                .mimeType(processed.mimeType()).formType(formType).formVersion(properties.getFormVersion())
                .languageHint(languageHint == null ? properties.getLanguageHint() : languageHint)
                .requestId(requestId).build();
        VisitorFormExtractionResult result;
        try {
            result = providerResolver.resolve(properties.getProvider()).extract(input);
        } catch (RuntimeException ex) {
            log.debug("Visitor form extraction failed requestId={} provider={} formType={} imageBytes={} durationMs={} success=false exception={}",
                    requestId, properties.getProvider(), formType, processed.bytes().length,
                    (System.nanoTime()-lifecycleStart)/1_000_000, ex.getClass().getSimpleName());
            throw ex;
        }
        ExtractedVisitorField<String> epic = result.getExtractedEpic();
        ExtractedVisitorField<String> name = result.getExtractedName();
        ExtractedVisitorField<String> mobile = result.getExtractedMobileNumber();
        ExtractedVisitorField<String> address = result.getExtractedAddress();
        validateEpic(epic); validateName(name); validateMobile(mobile); validateAddress(address);
        List<String> warnings = new ArrayList<>();
        warnings.addAll(result.getWarnings() == null ? List.of() : result.getWarnings());
        boolean review = result.isRequiresManualReview() || !name.isValid() || !mobile.isValid()
                || !epic.isValid() || !address.isValid() || uncertain(epic) || uncertain(name) || uncertain(mobile) || uncertain(address);
        int fieldsFound = (epic.getValue()!=null?1:0)+(name.getValue()!=null?1:0)+(mobile.getValue()!=null?1:0)+(address.getValue()!=null?1:0);
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
        log.debug("Visitor form extraction completed requestId={} provider={} model={} formType={} imageBytes={} imageWidth={} imageHeight={} success=true epicPresent={} epicMasked={} epicStatus={} epicValid={} namePresent={} nameMasked={} nameStatus={} nameValid={} mobilePresent={} mobileLast4={} mobileStatus={} mobileValid={} addressPresent={} addressLength={} addressStatus={} addressValid={} requiresManualReview={} warningCount={} durationMs={}",
                requestId, provider, model, formType, processed.bytes().length, processed.width(), processed.height(),
                present(epic), maskEpic(epic.getValue()), epic.getStatus(), epic.isValid(),
                present(name), maskName(name.getValue()), name.getStatus(), name.isValid(),
                present(mobile), mobileLast4(mobile.getValue()), mobile.getStatus(), mobile.isValid(),
                present(address), length(address.getValue()), address.getStatus(), address.isValid(),
                review, warnings.size(), (System.nanoTime()-lifecycleStart)/1_000_000);
        log.info("Visitor form extraction completed provider={} fieldsFound={} manualReview={} durationMs={} model={}",
                provider, fieldsFound, review, durationMs, model);
        return VisitorFormExtractionResponse.builder().success(true).documentType("VISITOR_REGISTRATION")
                .formVersion(properties.getFormVersion()).epic(epic).name(name).mobileNumber(mobile).address(address)
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
    private void validateEpic(ExtractedVisitorField<String> f) {
        String v=normalize(f.getValue()); if(v!=null) v=v.replaceAll("[^A-Za-z0-9]","").toUpperCase(Locale.ROOT); f.setValue(v);
        f.setValid(v!=null && v.matches("[A-Z]{3}[0-9]{7}"));
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
    private boolean present(ExtractedVisitorField<?> field) { return field != null && field.getValue() != null; }
    private int length(String value) { return value == null ? 0 : value.length(); }
    static String maskEpic(String value) { return maskKeepingLast(value, 4); }
    static String mobileLast4(String value) { return value == null || value.isBlank() ? "" : value.substring(Math.max(0,value.length()-4)); }
    static String maskName(String value) {
        if (value == null || value.isBlank()) return "";
        return value.substring(0,1) + "*".repeat(Math.max(0,value.length()-1));
    }
    private static String maskKeepingLast(String value,int visible) {
        if (value == null || value.isBlank()) return "";
        int hidden=Math.max(0,value.length()-visible);
        return "*".repeat(hidden)+value.substring(hidden);
    }
}
