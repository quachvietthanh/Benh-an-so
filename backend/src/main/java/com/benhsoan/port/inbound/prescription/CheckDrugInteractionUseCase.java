package com.benhsoan.port.inbound.prescription;

import java.util.List;

import com.benhsoan.port.dto.command.prescription.CheckDrugInteractionCommand;
import com.benhsoan.port.dto.result.DrugInteractionWarningResult;

public interface CheckDrugInteractionUseCase {

    List<DrugInteractionWarningResult> check(CheckDrugInteractionCommand command);
}
