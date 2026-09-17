package com.benhsoan.persistence.jpaRepository.clinical;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.clinical.enums.ClinicalOrderItemStatus;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;

public interface PendingClinicalOrderItemView {

    UUID getOrderItemId();

    UUID getOrderId();

    String getOrderCode();

    UUID getVisitId();

    String getVisitCode();

    UUID getPatientId();

    String getPatientCode();

    String getPatientFullName();

    UUID getDoctorId();

    String getDoctorFullName();

    UUID getClinicalServiceId();

    String getServiceCode();

    String getServiceName();

    ClinicalServiceType getServiceType();

    String getInstruction();

    String getClinicalReason();

    ClinicalOrderItemStatus getStatus();

    Instant getOrderedAt();
}
