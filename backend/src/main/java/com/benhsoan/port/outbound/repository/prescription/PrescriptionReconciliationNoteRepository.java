package com.benhsoan.port.outbound.repository.prescription;

import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.prescription.PrescriptionReconciliationNote;

/**
 * Append only persistence for NCL-12-CN-007 reconciliation notes.
 *
 * There is intentionally no update or delete operation: the notes are an immutable
 * history that explains why a discrepancy was accepted instead of retransmitted.
 */
public interface PrescriptionReconciliationNoteRepository {

    PrescriptionReconciliationNote save(PrescriptionReconciliationNote note);

    List<PrescriptionReconciliationNote> findByPrescriptionId(UUID prescriptionId);
}
