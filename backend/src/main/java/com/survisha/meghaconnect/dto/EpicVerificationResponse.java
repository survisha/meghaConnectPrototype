package com.survisha.meghaconnect.dto;

import com.survisha.meghaconnect.util.RequestContextUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO from Election Commission EPIC verification API.
 * 
 * Wraps the response from:
 *   https://devuat.offlinekyc.com/ECSOVDServiceV2/api/ovd/verify
 * 
 * External API Response Structure:
 * {
 *   "code": "200",
 *   "message": "Success",
 *   "data": {
 *     "voteridnumber": "BCV0259184",
 *     "borrowernameonvoteridcard": "MAREIAM MOSSANG",
 *     "relativenameonvoterid": "CHANLUNG MOSSANG",
 *     "borrowergender": "F",
 *     "borrowerdateofbirth": "",
 *     "borroweraddressstate": "Arunachal Pradesh",
 *     "borroweraddressdistrict": "CHANGLANG",
 *     "borroweraddresshousenumber": "Not Available",
 *     "borroweraddresssectionnumber": "2",
 *     "accountnumber": "51",
 *     "namematchscore": 5,
 *     "voteridverificationstatus": "id_found",
 *     "sourceinformation": "government_website",
 *     "pollingdetails": {
 *       "pollingpartno": "14",
 *       "pollingstationaddress": "NAMPONG TANGSA COMMUNITY HALL"
 *     },
 *     "voteridverificationrequestid": "8ffc7cbb-6287-4246-97e8-66fdf706ee63",
 *     "voteridverificationcompletiontimestamp": "27-04-2026 09:00:07"
 *   }
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EpicVerificationResponse {
    private String code;                      // HTTP status code (200, 400, etc.)
    private String message;                   // "Success" or error message
    private EpicVerificationData data;        // Strongly-typed response data
    private Boolean success;
    private Boolean canProceed;
    private String kycStatus;
    private String kycProvider;
    @Builder.Default
    private String requestId = RequestContextUtil.getRequestId();
    
    /**
     * Check if verification was successful
     */
    public boolean isSuccess() {
        return "200".equals(code) && "Success".equalsIgnoreCase(message) && data != null;
    }
    
    /**
     * Convenience: Get verified name from nested data
     */
    public String getVerifiedName() {
        return data != null ? data.getVerifiedName() : null;
    }
    
    /**
     * Convenience: Get district from nested data
     */
    public String getDistrict() {
        return data != null ? data.getDistrict() : null;
    }
    
    /**
     * Convenience: Get state from nested data
     */
    public String getState() {
        return data != null ? data.getState() : null;
    }
    
    /**
     * Convenience: Get name match score from nested data
     */
    public Integer getNameMatchScore() {
        return data != null ? data.getNameMatchScore() : null;
    }
    
    /**
     * Convenience: Check if ID was found
     */
    public boolean isIdFound() {
        return data != null && data.isIdFound();
    }
}
