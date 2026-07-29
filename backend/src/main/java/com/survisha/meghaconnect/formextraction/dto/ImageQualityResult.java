package com.survisha.meghaconnect.formextraction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageQualityResult {
    private boolean acceptable;
    private List<String> issues = new ArrayList<>();
}
