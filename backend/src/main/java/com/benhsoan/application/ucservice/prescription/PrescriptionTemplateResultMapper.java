package com.benhsoan.application.ucservice.prescription;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.prescription.PrescriptionTemplate;
import com.benhsoan.domain.prescription.PrescriptionTemplateItem;
import com.benhsoan.port.dto.result.PrescriptionTemplateResult;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PrescriptionTemplateResultMapper {

    private final MedicineRepository medicineRepository;
    private final DiagnosisCatalogRepository diagnosisCatalogRepository;

    public PrescriptionTemplateResult toResult(PrescriptionTemplate template) {
        if (template == null) {
            return null;
        }
        DiagnosisCatalog diagnosis = diagnosisCatalogRepository
                .findById(template.getDiagnosisCatalogId())
                .orElse(null);

        Map<UUID, Medicine> medicines = medicineRepository.findAllById(
                        template.getItems().stream()
                                .map(PrescriptionTemplateItem::getMedicineId)
                                .toList())
                .stream()
                .collect(Collectors.toMap(Medicine::getId, Function.identity()));

        List<PrescriptionTemplateResult.Item> items = template.getItems().stream()
                .map(item -> toItem(item, medicines.get(item.getMedicineId())))
                .toList();

        return new PrescriptionTemplateResult(
                template.getId(),
                template.getDiagnosisCatalogId(),
                diagnosis == null ? null : diagnosis.getCode(),
                diagnosis == null ? null : diagnosis.getName(),
                template.getCreatedBy(),
                template.getCreatedAt(),
                items
        );
    }

    private PrescriptionTemplateResult.Item toItem(PrescriptionTemplateItem item, Medicine medicine) {
        return new PrescriptionTemplateResult.Item(
                item.getId(),
                item.getMedicineId(),
                medicine == null ? null : medicine.getMedicineCode(),
                medicine == null ? null : medicine.getMedicineName(),
                medicine == null ? null : medicine.getActiveIngredient(),
                medicine == null ? null : medicine.getStrength(),
                medicine == null ? null : medicine.getUnit(),
                item.getDosage(),
                item.getFrequency(),
                item.getRoute(),
                item.getDurationDays(),
                item.getQuantity(),
                item.getInstructions(),
                item.getSortOrder()
        );
    }
}
