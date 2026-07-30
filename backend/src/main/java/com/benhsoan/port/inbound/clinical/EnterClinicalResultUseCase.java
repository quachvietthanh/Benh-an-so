package com.benhsoan.port.inbound.clinical; import java.util.UUID; import com.benhsoan.port.dto.command.clinical.EnterClinicalResultCommand; import com.benhsoan.port.dto.result.ClinicalResultResult;
public interface EnterClinicalResultUseCase { ClinicalResultResult enter(UUID clinicalOrderItemId, EnterClinicalResultCommand command); }
