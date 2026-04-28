package com.survisha.meghaconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Strongly-typed DTO for EPIC verification data from Election Commission API.
 * 
 * Maps directly to the "data" field in the API response:
 * {
 *   "voteridnumber": "BCV0259184",
 *   "borrowernameonvoteridcard": "MAREIAM MOSSANG",
 *   "relativenameonvoterid": "CHANLUNG MOSSANG",
 *   "borrowergender": "F",
 *   "borrowerdateofbirth": "",
 *   "borroweraddressstate": "Arunachal Pradesh",
 *   "borroweraddressdistrict": "CHANGLANG",
 *   "borroweraddresshousenumber": "Not Available",
 *   "borroweraddresssectionnumber": "2",
 *   "accountnumber": "51",
 *   "namematchscore": 5,
 *   "voteridverificationstatus": "id_found",
 *   "sourceinformation": "government_website",
 *   "pollingdetails": { ... },
 *   "voteridverificationrequestid": "8ffc7cbb-6287-4246-97e8-66fdf706ee63",
 *   "voteridverificationcompletiontimestamp": "27-04-2026 09:00:07"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EpicVerificationData {
    
    @JsonProperty("voteridnumber")
    private String voterIdNumber;
    
    @JsonProperty("borrowernameonvoteridcard")
    private String borrowerNameOnVoterIdCard;
    
    @JsonProperty("relativenameonvoterid")
    private String relativeNameOnVoterId;
    
    @JsonProperty("borrowergender")
    private String borrowerGender;
    
    @JsonProperty("borrowerdateofbirth")
    private String borrowerDateOfBirth;
    
    @JsonProperty("borroweraddressstate")
    private String borrowerAddressState;
    
    @JsonProperty("borroweraddressdistrict")
    private String borrowerAddressDistrict;
    
    @JsonProperty("borroweraddresshousenumber")
    private String borrowerAddressHouseNumber;
    
    @JsonProperty("borroweraddresssectionnumber")
    private String borrowerAddressSectionNumber;
    
    @JsonProperty("accountnumber")
    private String accountNumber;
    
    @JsonProperty("namematchscore")
    private Integer nameMatchScore;
    
    @JsonProperty("voteridverificationstatus")
    private String voterIdVerificationStatus;
    
    @JsonProperty("sourceinformation")
    private String sourceInformation;
    
    @JsonProperty("pollingdetails")
    private PollingDetails pollingDetails;
    
    @JsonProperty("voteridverificationrequestid")
    private String voterIdVerificationRequestId;
    
    @JsonProperty("voteridverificationcompletiontimestamp")
    private String voterIdVerificationCompletionTimestamp;
    
    // ─────────────────────────────────────────────────────────────────────────
    // Convenience accessors for controller/service usage
    // ─────────────────────────────────────────────────────────────────────────
    
    public String getVerifiedName() {
        return borrowerNameOnVoterIdCard;
    }
    
    public String getDistrict() {
        return borrowerAddressDistrict;
    }
    
    public String getState() {
        return borrowerAddressState;
    }
    
    public boolean isIdFound() {
        return "id_found".equalsIgnoreCase(voterIdVerificationStatus);
    }
}
