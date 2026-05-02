package com.survisha.meghaconnect.dto;

import lombok.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicDarbarRequest {

    @NotNull(message = "darbarDate is required")
    private LocalDate darbarDate;

    @NotBlank(message = "location is required")
    private String location;

    @NotNull(message = "maxSlots is required")
    @Min(value = 1, message = "maxSlots must be at least 1")
    private Integer maxSlots;

    private String remarks;
}
