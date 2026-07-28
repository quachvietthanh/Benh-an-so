package com.benhsoan.infrastructure.authSecurity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Base64;

import org.junit.jupiter.api.Test;

class SecureRefreshTokenGeneratorAdapterTest {

    private final SecureRefreshTokenGeneratorAdapter generator = new SecureRefreshTokenGeneratorAdapter();

    @Test
    void generatesIndependent256BitUrlSafeTokens() {
        String firstToken = generator.generate();
        String secondToken = generator.generate();

        assertEquals(32, Base64.getUrlDecoder().decode(firstToken).length);
        assertEquals(32, Base64.getUrlDecoder().decode(secondToken).length);
        assertNotEquals(firstToken, secondToken);
    }
}
