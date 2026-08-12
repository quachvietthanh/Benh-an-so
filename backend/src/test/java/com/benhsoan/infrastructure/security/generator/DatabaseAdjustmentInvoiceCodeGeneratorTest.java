package com.benhsoan.infrastructure.security.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.port.outbound.repository.billing.InvoiceCodeSequenceRepository;

@ExtendWith(MockitoExtension.class)
class DatabaseAdjustmentInvoiceCodeGeneratorTest {

    @Mock private InvoiceCodeSequenceRepository sequenceRepository;

    @Test
    void generatesFirstAdjustmentInvoiceCodeWhenNoInvoiceExists() {
        when(sequenceRepository.reserveNextValue("HDDC")).thenReturn(1L);

        assertEquals(
                "HDDC000001",
                new DatabaseAdjustmentInvoiceCodeGenerator(sequenceRepository).generate()
        );
    }

    @Test
    void incrementsTheLatestAdjustmentInvoiceCode() {
        when(sequenceRepository.reserveNextValue("HDDC")).thenReturn(7L);

        assertEquals(
                "HDDC000007",
                new DatabaseAdjustmentInvoiceCodeGenerator(sequenceRepository).generate()
        );
    }
}
