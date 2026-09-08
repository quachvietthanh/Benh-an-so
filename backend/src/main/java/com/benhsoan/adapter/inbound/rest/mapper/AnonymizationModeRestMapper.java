package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.anonymization.UpdateAnonymizationModeRequest;
import com.benhsoan.adapter.inbound.rest.response.anonymization.AnonymizationModeResponse;
import com.benhsoan.port.dto.command.anonymization.UpdateAnonymizationModeCommand;
import com.benhsoan.port.dto.result.anonymization.AnonymizationModeResult;

@Component
public class AnonymizationModeRestMapper {

    public UpdateAnonymizationModeCommand toCommand(UpdateAnonymizationModeRequest request) {
        return new UpdateAnonymizationModeCommand(request.enabled());
    }

    public AnonymizationModeResponse toResponse(AnonymizationModeResult result) {
        return new AnonymizationModeResponse(result.enabled(), result.updatedAt());
    }
}
