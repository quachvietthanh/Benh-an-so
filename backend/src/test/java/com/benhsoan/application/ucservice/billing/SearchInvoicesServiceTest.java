package com.benhsoan.application.ucservice.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.benhsoan.domain.billing.Invoice;
import com.benhsoan.port.dto.command.billing.SearchInvoicesQuery;
import com.benhsoan.port.outbound.repository.billing.InvoiceRepository;
import com.benhsoan.port.outbound.repository.billing.InvoiceSearchCriteria;

class SearchInvoicesServiceTest {

    private final InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
    private final InvoiceResultMapper resultMapper = mock(InvoiceResultMapper.class);

    private SearchInvoicesService service;

    @BeforeEach
    void setUp() {
        service = new SearchInvoicesService(invoiceRepository, resultMapper);
    }

    @Test
    void forwardsPatientNameAndDateRangeIntoSearchCriteria() {
        when(invoiceRepository.search(any(), any()))
                .thenReturn(new PageImpl<Invoice>(List.of()));

        service.search(new SearchInvoicesQuery(
                null, null, null, "Nguyen", null, null, PageRequest.of(0, 20)));

        ArgumentCaptor<InvoiceSearchCriteria> captor =
                ArgumentCaptor.forClass(InvoiceSearchCriteria.class);
        verify(invoiceRepository).search(captor.capture(), any());
        assertEquals("Nguyen", captor.getValue().patientName());
    }

    @Test
    void forwardsNullPatientNameWhenNotProvided() {
        when(invoiceRepository.search(any(), any()))
                .thenReturn(new PageImpl<Invoice>(List.of()));

        service.search(new SearchInvoicesQuery(
                "HD000010", null, null, null, null, null, PageRequest.of(0, 20)));

        ArgumentCaptor<InvoiceSearchCriteria> captor =
                ArgumentCaptor.forClass(InvoiceSearchCriteria.class);
        verify(invoiceRepository).search(captor.capture(), any());
        assertEquals("HD000010", captor.getValue().invoiceCode());
        assertEquals(null, captor.getValue().patientName());
    }
}
