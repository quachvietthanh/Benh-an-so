package com.benhsoan.domain.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TemporaryPasswordGeneratorTest {

    @Test
    @DisplayName("generated temporary password satisfies password policy")
    void generatedPasswordMeetsPolicy() {
        for (int i = 0; i < 20; i++) {
            String tempPassword = TemporaryPasswordGenerator.generate();
            assertNotNull(tempPassword);
            assertTrue(tempPassword.length() >= 8);
            assertDoesNotThrow(() -> PasswordPolicyValidator.validateOrThrow(tempPassword));
        }
    }
}
