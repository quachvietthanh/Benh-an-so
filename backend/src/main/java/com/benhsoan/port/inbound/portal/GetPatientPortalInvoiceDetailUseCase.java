package com.benhsoan.port.inbound.portal;

import java.util.UUID;

import com.benhsoan.port.dto.result.portal.PatientPortalInvoiceDetailResult;

public interface GetPatientPortalInvoiceDetailUseCase {

    PatientPortalInvoiceDetailResult getInvoiceDetail(UUID invoiceId);
}
