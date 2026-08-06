package com.benhsoan.exception;

import java.time.Instant;
import java.util.List;

import com.benhsoan.domain.prescription.exception.PrescriptionInteractionConfirmationRequiredException.InteractionWarning;

public record InteractionConflictResponse(

        Instant timestamp,

        int status,

        String error,

        String message,

        String path,

        List<InteractionWarning> warnings

) {
}
