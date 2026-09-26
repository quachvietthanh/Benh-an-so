package com.benhsoan.infrastructure.security.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.port.outbound.repository.billing.InvoiceCodeSequenceRepository;

@ExtendWith(MockitoExtension.class)
class DatabaseInvoiceCodeGeneratorTest {

    @Mock private InvoiceCodeSequenceRepository sequenceRepository;

    @Test
    void generatesFirstInvoiceCodeWhenNoInvoiceExists() {
        when(sequenceRepository.reserveNextValue("HD")).thenReturn(1L);

        assertEquals("HD000001", new DatabaseInvoiceCodeGenerator(sequenceRepository).generate());
    }

    @Test
    void incrementsTheLatestInvoiceCode() {
        when(sequenceRepository.reserveNextValue("HD")).thenReturn(12L);

        assertEquals("HD000012", new DatabaseInvoiceCodeGenerator(sequenceRepository).generate());
    }
}
