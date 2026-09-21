package com.benhsoan.application.ucservice.controlledmedicine;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.controlledmedicine.ControlledMedicineRegister;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.controlledmedicine.SearchControlledMedicineRegisterQuery;
import com.benhsoan.port.dto.result.ControlledMedicineRegisterResult;
import com.benhsoan.port.inbound.controlledmedicine.SearchControlledMedicineRegisterUseCase;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.controlledmedicine.ControlledMedicineRegisterRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchControlledMedicineRegisterService
        implements SearchControlledMedicineRegisterUseCase {

    private final ControlledMedicineRegisterRepository registerRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    @Override
    public Page<ControlledMedicineRegisterResult> search(
            SearchControlledMedicineRegisterQuery query
    ) {
        requireQuery(query);

        Page<ControlledMedicineRegister> page = registerRepository.search(
                new ControlledMedicineRegisterRepository.ControlledMedicineRegisterSearchCriteria(
                        query.patientId(),
                        query.medicineId(),
                        query.from(),
                        query.to()
                ),
                PageRequest.of(query.page(), query.size())
        );

        Map<UUID, Patient> patientsById = resolvePatients(page.getContent());
        Map<UUID, User> usersById = resolveUsers(page.getContent());

        return page.map(record -> toResult(record, patientsById, usersById));
    }

    private Map<UUID, Patient> resolvePatients(List<ControlledMedicineRegister> records) {
        List<UUID> patientIds = records.stream()
                .map(ControlledMedicineRegister::getPatientId)
                .distinct()
                .toList();
        if (patientIds.isEmpty()) {
            return Map.of();
        }
        return patientRepository.findAllById(patientIds).stream()
                .collect(Collectors.toMap(Patient::getId, Function.identity()));
    }

    private Map<UUID, User> resolveUsers(List<ControlledMedicineRegister> records) {
        List<UUID> userIds = records.stream()
                .flatMap(record -> java.util.stream.Stream.of(
                        record.getPrescribedBy(),
                        record.getDispensedBy()
                ))
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private ControlledMedicineRegisterResult toResult(
            ControlledMedicineRegister record,
            Map<UUID, Patient> patientsById,
            Map<UUID, User> usersById
    ) {
        Patient patient = patientsById.get(record.getPatientId());
        User doctor = usersById.get(record.getPrescribedBy());
        User pharmacist = usersById.get(record.getDispensedBy());

        return new ControlledMedicineRegisterResult(
                record.getId(),
                record.getPrescriptionId(),
                record.getPrescriptionItemId(),
                record.getMedicineId(),
                record.getMedicineName(),
                record.getPatientId(),
                patient == null ? null : patient.getPatientCode(),
                patient == null ? null : patient.getFullName(),
                record.getPrescribedBy(),
                doctor == null ? null : doctor.getFullName(),
                record.getDispensedBy(),
                pharmacist == null ? null : pharmacist.getFullName(),
                record.getQuantity(),
                record.getDispensedAt()
        );
    }

    private void requireQuery(SearchControlledMedicineRegisterQuery query) {
        if (query == null) {
            throw new ValidationException("Search query is required.");
        }
        if (query.page() < 0) {
            throw new ValidationException("Page must not be negative.");
        }
        if (query.size() <= 0) {
            throw new ValidationException("Size must be greater than zero.");
        }
    }
}
