package com.survisha.meghaconnect.legacy.dto;
import lombok.Data;

@Data
public class LegacyPersonSearchRequest {
    private String epic;
    private String name;
    private String mobile;
    private String village;
    private String address;
    private String district;
    private String constituency;
    private Integer page;
    private Integer limit;
}
