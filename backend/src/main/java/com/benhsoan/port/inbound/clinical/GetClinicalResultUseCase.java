package com.benhsoan.port.inbound.clinical; import java.util.UUID; import com.benhsoan.port.dto.result.ClinicalResultResult;
public interface GetClinicalResultUseCase { ClinicalResultResult getById(UUID clinicalResultId); }
