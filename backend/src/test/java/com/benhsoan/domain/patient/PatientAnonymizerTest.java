package com.benhsoan.domain.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PatientAnonymizerTest {

    @Test
    void masksFullNameWithPatientCode() {
        assertEquals("BỆNH NHÂN #BN001", PatientAnonymizer.maskFullName("BN001"));
    }

    @Test
    void masksFullNameWithoutPatientCodeToGenericLabel() {
        assertEquals("BỆNH NHÂN", PatientAnonymizer.maskFullName(null));
        assertEquals("BỆNH NHÂN", PatientAnonymizer.maskFullName("   "));
    }

    @Test
    void masksPhoneKeepingFirstAndLastTwoDigits() {
        assertEquals("09******78", PatientAnonymizer.maskPhone("0912345678"));
    }

    @Test
    void masksPhoneNormalizingFormatting() {
        assertEquals("09******78", PatientAnonymizer.maskPhone("0912 345 678"));
        assertEquals("09******78", PatientAnonymizer.maskPhone("0912-345-678"));
        assertEquals("84******78", PatientAnonymizer.maskPhone("+84 912 345 678"));
    }

    @Test
    void masksShortPhoneWithPlaceholder() {
        assertEquals("******", PatientAnonymizer.maskPhone("123"));
    }

    @Test
    void masksAddress() {
        assertEquals("[ĐỊA CHỈ ĐÃ ẨN DANH]", PatientAnonymizer.maskAddress("123 Nguyễn Trãi, Hà Nội"));
    }

    @Test
    void handlesNullAndBlankInputs() {
        assertNull(PatientAnonymizer.maskPhone(null));
        assertEquals("   ", PatientAnonymizer.maskPhone("   "));
        assertNull(PatientAnonymizer.maskAddress(null));
        assertEquals("   ", PatientAnonymizer.maskAddress("   "));
    }

    @Test
    void masksPublicPortalPhoneWithHistoricalContract() {
        assertEquals("091***678", PatientAnonymizer.maskPhonePublicPortal("0912345678"));
        assertEquals("***", PatientAnonymizer.maskPhonePublicPortal("123456"));
        assertNull(PatientAnonymizer.maskPhonePublicPortal(null));
    }

    @Test
    void detectsMaskedValues() {
        assertTrue(PatientAnonymizer.isMaskedFullName("BỆNH NHÂN #BN001"));
        assertTrue(PatientAnonymizer.isMaskedFullName("BỆNH NHÂN"));
        assertFalse(PatientAnonymizer.isMaskedFullName("Nguyễn Văn A"));

        assertTrue(PatientAnonymizer.isMaskedPhone("09******78"));
        assertFalse(PatientAnonymizer.isMaskedPhone("0912345678"));

        assertTrue(PatientAnonymizer.isMaskedAddress("[ĐỊA CHỈ ĐÃ ẨN DANH]"));
        assertFalse(PatientAnonymizer.isMaskedAddress("123 Nguyễn Trãi"));
    }
}
