package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.inventory.ApproveProcurementPlanRequest;
import com.benhsoan.adapter.inbound.rest.request.inventory.CreateProcurementPlanItemRequest;
import com.benhsoan.adapter.inbound.rest.request.inventory.CreateProcurementPlanRequest;
import com.benhsoan.adapter.inbound.rest.request.inventory.RejectProcurementPlanRequest;
import com.benhsoan.adapter.inbound.rest.request.inventory.UpdateProcurementPlanRequest;
import com.benhsoan.adapter.inbound.rest.response.inventory.ProcurementPlanItemResponse;
import com.benhsoan.adapter.inbound.rest.response.inventory.ProcurementPlanResponse;
import com.benhsoan.adapter.inbound.rest.response.inventory.ProcurementPlanSummaryResponse;
import com.benhsoan.adapter.inbound.rest.response.inventory.ProcurementSuggestionItemResponse;
import com.benhsoan.adapter.inbound.rest.response.inventory.ProcurementSuggestionResponse;
import com.benhsoan.port.dto.command.inventory.ApproveProcurementPlanCommand;
import com.benhsoan.port.dto.command.inventory.CreateProcurementPlanCommand;
import com.benhsoan.port.dto.command.inventory.CreateProcurementPlanItemCommand;
import com.benhsoan.port.dto.command.inventory.RejectProcurementPlanCommand;
import com.benhsoan.port.dto.command.inventory.UpdateProcurementPlanCommand;
import com.benhsoan.port.dto.result.ProcurementPlanItemResult;
import com.benhsoan.port.dto.result.ProcurementPlanResult;
import com.benhsoan.port.dto.result.ProcurementPlanSummaryResult;
import com.benhsoan.port.dto.result.ProcurementSuggestionItemResult;
import com.benhsoan.port.dto.result.ProcurementSuggestionResult;

@Component
public class MedicationProcurementRestMapper {

    public CreateProcurementPlanCommand toCreateCommand(CreateProcurementPlanRequest request) {
        if (request == null) {
            return null;
        }

        List<CreateProcurementPlanItemCommand> items = new ArrayList<>();
        if (request.items() != null) {
            for (CreateProcurementPlanItemRequest itemReq : request.items()) {
                items.add(new CreateProcurementPlanItemCommand(
                        itemReq.medicineId(),
                        itemReq.currentStock(),
                        itemReq.minStockThreshold(),
                        itemReq.previousPeriodConsumption(),
                        itemReq.suggestedQuantity(),
                        itemReq.proposedQuantity(),
                        itemReq.note()
                ));
            }
        }

        return new CreateProcurementPlanCommand(
                request.periodStartDate(),
                request.periodEndDate(),
                request.note(),
                Boolean.TRUE.equals(request.submitImmediately()),
                items
        );
    }

    public UpdateProcurementPlanCommand toUpdateCommand(UUID planId, UpdateProcurementPlanRequest request) {
        if (request == null) {
            return null;
        }

        List<CreateProcurementPlanItemCommand> items = new ArrayList<>();
        if (request.items() != null) {
            for (CreateProcurementPlanItemRequest itemReq : request.items()) {
                items.add(new CreateProcurementPlanItemCommand(
                        itemReq.medicineId(),
                        itemReq.currentStock(),
                        itemReq.minStockThreshold(),
                        itemReq.previousPeriodConsumption(),
                        itemReq.suggestedQuantity(),
                        itemReq.proposedQuantity(),
                        itemReq.note()
                ));
            }
        }

        return new UpdateProcurementPlanCommand(
                planId,
                request.note(),
                items
        );
    }

    public ApproveProcurementPlanCommand toApproveCommand(UUID planId, ApproveProcurementPlanRequest request) {
        return new ApproveProcurementPlanCommand(
                planId,
                request != null ? request.note() : null,
                request != null ? request.itemAdjustments() : null
        );
    }

    public RejectProcurementPlanCommand toRejectCommand(UUID planId, RejectProcurementPlanRequest request) {
        return new RejectProcurementPlanCommand(
                planId,
                request != null ? request.reason() : null
        );
    }

    public ProcurementSuggestionResponse toSuggestionResponse(ProcurementSuggestionResult result) {
        if (result == null) {
            return null;
        }

        List<ProcurementSuggestionItemResponse> items = new ArrayList<>();
        if (result.items() != null) {
            for (ProcurementSuggestionItemResult item : result.items()) {
                items.add(new ProcurementSuggestionItemResponse(
                        item.medicineId(),
                        item.medicineCode(),
                        item.medicineName(),
                        item.unit(),
                        item.currentStock(),
                        item.eligibleStock(),
                        item.minStockThreshold(),
                        item.previousPeriodConsumption(),
                        item.suggestedQuantity()
                ));
            }
        }

        return new ProcurementSuggestionResponse(
                result.periodStartDate(),
                result.periodEndDate(),
                result.calculatedAt(),
                result.totalItems(),
                items
        );
    }

    public ProcurementPlanResponse toPlanResponse(ProcurementPlanResult result) {
        if (result == null) {
            return null;
        }

        List<ProcurementPlanItemResponse> items = new ArrayList<>();
        if (result.items() != null) {
            for (ProcurementPlanItemResult item : result.items()) {
                items.add(new ProcurementPlanItemResponse(
                        item.id(),
                        item.planId(),
                        item.medicineId(),
                        item.medicineCode(),
                        item.medicineName(),
                        item.unit(),
                        item.currentStock(),
                        item.minStockThreshold(),
                        item.previousPeriodConsumption(),
                        item.suggestedQuantity(),
                        item.proposedQuantity(),
                        item.approvedQuantity(),
                        item.note(),
                        item.createdAt()
                ));
            }
        }

        return new ProcurementPlanResponse(
                result.id(),
                result.planCode(),
                result.status(),
                result.createdBy(),
                result.periodStartDate(),
                result.periodEndDate(),
                result.totalItems(),
                result.totalSuggestedQuantity(),
                result.totalProposedQuantity(),
                result.totalApprovedQuantity(),
                result.note(),
                result.submittedAt(),
                result.approvedBy(),
                result.approvedAt(),
                result.rejectionReason(),
                result.createdAt(),
                result.updatedAt(),
                items
        );
    }

    public ProcurementPlanSummaryResponse toSummaryResponse(ProcurementPlanSummaryResult result) {
        if (result == null) {
            return null;
        }

        return new ProcurementPlanSummaryResponse(
                result.id(),
                result.planCode(),
                result.status(),
                result.createdBy(),
                result.periodStartDate(),
                result.periodEndDate(),
                result.totalItems(),
                result.totalProposedQuantity(),
                result.totalApprovedQuantity(),
                result.submittedAt(),
                result.approvedBy(),
                result.approvedAt(),
                result.createdAt()
        );
    }
}
