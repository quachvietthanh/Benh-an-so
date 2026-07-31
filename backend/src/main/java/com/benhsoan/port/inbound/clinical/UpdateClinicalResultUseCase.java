package com.benhsoan.port.inbound.clinical; import java.util.UUID; import com.benhsoan.port.dto.command.clinical.UpdateClinicalResultCommand; import com.benhsoan.port.dto.result.ClinicalResultResult;
public interface UpdateClinicalResultUseCase { ClinicalResultResult update(UUID clinicalResultId, UpdateClinicalResultCommand command); }
