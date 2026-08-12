package com.benhsoan.application.ucservice.billing;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.command.billing.SearchInvoicesQuery;
import com.benhsoan.port.dto.result.InvoiceResult;
import com.benhsoan.port.inbound.billing.SearchInvoicesUseCase;
import com.benhsoan.port.outbound.repository.billing.InvoiceRepository;
import com.benhsoan.port.outbound.repository.billing.InvoiceSearchCriteria;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchInvoicesService implements SearchInvoicesUseCase {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceResultMapper resultMapper;

    @Override
    public Page<InvoiceResult> search(SearchInvoicesQuery query) {
        InvoiceSearchCriteria criteria = new InvoiceSearchCriteria(
                query.invoiceCode(),
                query.invoiceType(),
                query.visitId(),
                query.createdFrom(),
                query.createdTo()
        );

        return invoiceRepository.search(criteria, query.pageable())
                .map(resultMapper::toResult);
    }
}
