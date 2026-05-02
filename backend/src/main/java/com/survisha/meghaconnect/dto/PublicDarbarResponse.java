package com.survisha.meghaconnect.dto;

import com.survisha.meghaconnect.entity.PublicDarbar;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicDarbarResponse {

    private Long id;
    private LocalDate darbarDate;
    private String location;
    private Integer maxSlots;
    private PublicDarbar.DarbarStatus status;
    private String activatedBy;
    private LocalDateTime activatedAt;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
