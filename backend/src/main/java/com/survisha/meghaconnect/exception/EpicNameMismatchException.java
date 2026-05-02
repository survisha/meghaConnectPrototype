package com.survisha.meghaconnect.exception;

import com.survisha.meghaconnect.dto.EpicVerificationData;

/**
 * Exception thrown when the name entered by the applicant does not match the
 * name returned by the EPIC verification provider.
 */
public class EpicNameMismatchException extends MeghaConnectException {

    private final EpicVerificationData verificationData;

    public EpicNameMismatchException(EpicVerificationData verificationData) {
        super(
                ErrorCodeConstants.EPIC_NAME_MISMATCH,
                ErrorCodeConstants.EPIC_NAME_MISMATCH_MSG,
                400
        );
        this.verificationData = verificationData;
    }

    public EpicVerificationData getVerificationData() {
        return verificationData;
    }
}
