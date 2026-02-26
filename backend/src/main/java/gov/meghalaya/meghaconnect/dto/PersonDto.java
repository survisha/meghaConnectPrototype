package gov.meghalaya.meghaconnect.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PersonDto {
    private Long id;
    private String fullName;
    private String phoneNumber;
    private String epicNumber;
    private String designation;
    private String district;
    private String constituency;
    private String booth;
    private String village;
    private String briefProfile;
}
