package com.benhsoan.infrastructure.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Mock Patient Verification Code Adapter Tests")
class MockPatientVerificationCodeAdapterTest {

    private MockPatientVerificationCodeAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MockPatientVerificationCodeAdapter();
    }

    @Test
    @DisplayName("Should store and retrieve last sent code for testing")
    void shouldStoreAndRetrieveCode() {
        String phone = "0901234567";
        String code = "123456";

        adapter.sendVerificationCode(phone, code, 300);

        assertEquals(code, adapter.getLastSentCode(phone));
    }

    @Test
    @DisplayName("Should clear stored codes")
    void shouldClearStoredCodes() {
        String phone = "0901234567";
        adapter.sendVerificationCode(phone, "123456", 300);
        adapter.clear();

        assertNull(adapter.getLastSentCode(phone));
    }
}
