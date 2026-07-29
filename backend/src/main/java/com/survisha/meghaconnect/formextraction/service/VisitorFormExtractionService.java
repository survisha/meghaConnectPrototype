package com.survisha.meghaconnect.formextraction.service;

import com.survisha.meghaconnect.formextraction.dto.VisitorFormExtractionResponse;
import org.springframework.web.multipart.MultipartFile;

public interface VisitorFormExtractionService {
    VisitorFormExtractionResponse extract(MultipartFile image, String formType, String languageHint, String actor);
}
