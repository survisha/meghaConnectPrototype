package com.survisha.meghaconnect.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class OcrService {

    private final DocumentExtractionService documentExtractionService;

    public String extractText(MultipartFile file) {
        return documentExtractionService.extractText(file);
    }
}
