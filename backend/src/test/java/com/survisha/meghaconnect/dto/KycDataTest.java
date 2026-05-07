package com.survisha.meghaconnect.dto;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KycDataTest {

    @Test
    void successSplitsRegionalAddressIntoResponseFields() {
        KycData kycData = KycData.success(
                "txn-123",
                Map.of(KycData.CLAIM_REG_ADDRESS, "123 Main St, Bengaluru Karnataka 560001")
        );

        assertEquals("123 Main St", kycData.getAddress1());
        assertEquals("Bengaluru", kycData.getCity());
        assertEquals("Karnataka", kycData.getState());
        assertEquals("560001", kycData.getPincode());
        assertEquals("123 Main St, Bengaluru Karnataka 560001", kycData.getAddress());
    }
}
