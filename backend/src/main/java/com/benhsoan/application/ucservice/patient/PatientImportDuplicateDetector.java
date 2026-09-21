package com.benhsoan.application.ucservice.patient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.shared.VietnameseTextNormalizer;
import com.benhsoan.port.dto.result.patient.SuspectedDuplicateResult;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PatientImportDuplicateDetector {

    private final PatientRepository patientRepository;

    private record GroupKey(String normalizedName, LocalDate dob, String phone) {}

    public record DuplicateCheckResult(
            List<ValidatedPatientRowDto> nonDuplicateRows,
            List<SuspectedDuplicateResult> suspectedDuplicates
    ) {}

    public DuplicateCheckResult detectDuplicates(List<ValidatedPatientRowDto> validRows) {
        List<ValidatedPatientRowDto> nonDuplicateRows = new ArrayList<>();
        List<SuspectedDuplicateResult> suspectedDuplicates = new ArrayList<>();

        Set<String> seenIdentityNumbers = new HashSet<>();
        Set<GroupKey> seenGroupKeys = new HashSet<>();
        Map<String, Boolean> identityExistsCache = new HashMap<>();
        Map<String, List<Patient>> phonePatientsCache = new HashMap<>();

        for (ValidatedPatientRowDto row : validRows) {
            String normName = row.getFullName() == null ? "" : VietnameseTextNormalizer.normalize(row.getFullName());
            String phone = row.getPhone() == null ? "" : row.getPhone();
            GroupKey key = new GroupKey(normName, row.getDateOfBirth(), phone);

            // 1. Check Internal Duplicates within the same file
            if (row.getIdentityNumber() != null && seenIdentityNumbers.contains(row.getIdentityNumber())) {
                suspectedDuplicates.add(new SuspectedDuplicateResult(
                        row.getRowNumber(),
                        row.getFullName(),
                        row.getDateOfBirth(),
                        row.getPhone(),
                        row.getIdentityNumber(),
                        null,
                        null,
                        null,
                        "Trùng số CCCD/CMND với một dòng khác trong cùng tệp tải lên."
                ));
                continue;
            }

            if (seenGroupKeys.contains(key)) {
                suspectedDuplicates.add(new SuspectedDuplicateResult(
                        row.getRowNumber(),
                        row.getFullName(),
                        row.getDateOfBirth(),
                        row.getPhone(),
                        row.getIdentityNumber(),
                        null,
                        null,
                        null,
                        phone.isEmpty()
                                ? "Trùng Họ tên và Ngày sinh với một dòng khác trong cùng tệp tải lên."
                                : "Trùng Họ tên, Ngày sinh và Số điện thoại với một dòng khác trong cùng tệp tải lên."
                ));
                continue;
            }

            // 2. Check Database Duplicates by CCCD/CMND (using cache to avoid repeated queries)
            if (row.getIdentityNumber() != null) {
                boolean existsInDb = identityExistsCache.computeIfAbsent(
                        row.getIdentityNumber(),
                        patientRepository::existsByIdentityNumber
                );
                if (existsInDb) {
                    suspectedDuplicates.add(new SuspectedDuplicateResult(
                            row.getRowNumber(),
                            row.getFullName(),
                            row.getDateOfBirth(),
                            row.getPhone(),
                            row.getIdentityNumber(),
                            null,
                            null,
                            null,
                            "Số CCCD/CMND đã tồn tại trên hệ thống."
                    ));
                    continue;
                }
            }

            // 3. Check Database Duplicates by (Name + DOB + Phone)
            boolean matchedDb = false;
            if (!phone.isEmpty()) {
                List<Patient> existingByPhone = phonePatientsCache.computeIfAbsent(
                        phone,
                        patientRepository::findAllByPhone
                );
                for (Patient p : existingByPhone) {
                    if (p.isMerged()) {
                        continue;
                    }
                    boolean dobMatch = p.getDateOfBirth() != null && p.getDateOfBirth().equals(row.getDateOfBirth());
                    String existingNormName = p.getFullName() == null ? "" : VietnameseTextNormalizer.normalize(p.getFullName());
                    boolean nameMatch = normName.equalsIgnoreCase(existingNormName);

                    if (dobMatch && nameMatch) {
                        suspectedDuplicates.add(new SuspectedDuplicateResult(
                                row.getRowNumber(),
                                row.getFullName(),
                                row.getDateOfBirth(),
                                row.getPhone(),
                                row.getIdentityNumber(),
                                p.getId(),
                                p.getPatientCode(),
                                p.getFullName(),
                                "Hồ sơ trùng khớp Họ tên, Ngày sinh và Số điện thoại với bệnh nhân " + p.getPatientCode() + " đã có trong hệ thống."
                        ));
                        matchedDb = true;
                        break;
                    }
                }
            }

            if (matchedDb) {
                continue;
            }

            // Record as seen
            if (row.getIdentityNumber() != null) {
                seenIdentityNumbers.add(row.getIdentityNumber());
            }
            seenGroupKeys.add(key);

            nonDuplicateRows.add(row);
        }

        return new DuplicateCheckResult(nonDuplicateRows, suspectedDuplicates);
    }
}
