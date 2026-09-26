package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.clinical.ClinicalReferenceRangeRequest;
import com.benhsoan.adapter.inbound.rest.request.clinical.CreateClinicalServiceRequest;
import com.benhsoan.adapter.inbound.rest.request.clinical.UpdateClinicalServiceRequest;
import com.benhsoan.adapter.inbound.rest.response.clinical.ClinicalReferenceRangeResponse;
import com.benhsoan.adapter.inbound.rest.response.clinical.ClinicalServiceManagementResponse;
import com.benhsoan.port.dto.command.clinical.CreateClinicalReferenceRangeCommand;
import com.benhsoan.port.dto.command.clinical.CreateClinicalServiceCommand;
import com.benhsoan.port.dto.command.clinical.UpdateClinicalReferenceRangeCommand;
import com.benhsoan.port.dto.command.clinical.UpdateClinicalServiceCommand;
import com.benhsoan.port.dto.result.ClinicalReferenceRangeResult;
import com.benhsoan.port.dto.result.ClinicalServiceManagementResult;

@Component
public class ClinicalServiceCatalogManagementRestMapper {

    public CreateClinicalServiceCommand toCommand(CreateClinicalServiceRequest request) {
        return new CreateClinicalServiceCommand(
                request.serviceCatalogId(), request.serviceCode(), request.serviceName(), request.serviceType(),
                request.resultDataType(), request.unit(), request.referenceRange(), request.description()
        );
    }

    public UpdateClinicalServiceCommand toCommand(UpdateClinicalServiceRequest request) {
        return new UpdateClinicalServiceCommand(
                request.serviceName(), request.serviceType(), request.resultDataType(),
                request.unit(), request.referenceRange(), request.description()
        );
    }

    public CreateClinicalReferenceRangeCommand toCreateCommand(ClinicalReferenceRangeRequest request) {
        return new CreateClinicalReferenceRangeCommand(
                request.gender(), request.minAge(), request.maxAge(), request.lowerBound(), request.upperBound()
        );
    }

    public UpdateClinicalReferenceRangeCommand toUpdateCommand(ClinicalReferenceRangeRequest request) {
        return new UpdateClinicalReferenceRangeCommand(
                request.gender(), request.minAge(), request.maxAge(), request.lowerBound(), request.upperBound()
        );
    }

    public ClinicalServiceManagementResponse toResponse(ClinicalServiceManagementResult result) {
        return new ClinicalServiceManagementResponse(
                result.id(), result.serviceCatalogId(), result.serviceCode(), result.serviceName(),
                result.serviceType(), result.resultDataType(), result.unit(), result.referenceRange(),
                result.description(), result.active(), result.createdAt(), result.updatedAt(),
                result.referenceRanges().stream().map(this::toResponse).toList()
        );
    }

    public ClinicalReferenceRangeResponse toResponse(ClinicalReferenceRangeResult result) {
        return new ClinicalReferenceRangeResponse(
                result.id(), result.clinicalServiceId(), result.gender(), result.minAge(), result.maxAge(),
                result.lowerBound(), result.upperBound(), result.active(), result.createdAt(), result.updatedAt()
        );
    }
}
