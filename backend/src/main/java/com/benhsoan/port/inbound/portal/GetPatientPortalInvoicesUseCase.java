package com.benhsoan.port.inbound.portal;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.portal.PatientPortalInvoiceSummaryResult;

public interface GetPatientPortalInvoicesUseCase {

    default List<PatientPortalInvoiceSummaryResult> getInvoices(UUID visitId) {
        return getInvoices(visitId, null);
    }

    List<PatientPortalInvoiceSummaryResult> getInvoices(UUID visitId, Integer limit);
}
