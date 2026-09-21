package com.benhsoan.adapter.inbound.rest.request.patient;

import com.benhsoan.domain.patient.enums.PregnancyStatus;

public record UpdatePatientPregnancyStatusRequest(
        PregnancyStatus pregnancyStatus
) {
}
