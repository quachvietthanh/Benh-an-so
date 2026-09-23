package com.benhsoan.port.dto.command.patient;

import lombok.Builder;

@Builder
public record RequestPatientDataErasureCommand(

        String reason

) {
}
