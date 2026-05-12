package com.survisha.meghaconnect.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeApplicationItemDto {
    private Long id;
    private String description;
    private Integer quantity;
    private BigDecimal unitCost;
    private BigDecimal cmoModeratedUnitCost;
    private BigDecimal hcmApprovedUnitCost;
}
