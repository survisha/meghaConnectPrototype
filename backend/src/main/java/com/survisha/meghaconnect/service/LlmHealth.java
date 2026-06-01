package com.survisha.meghaconnect.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmHealth {
    private String provider;
    private String model;
    private boolean available;
    private String message;
}
