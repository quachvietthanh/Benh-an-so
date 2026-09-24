package com.benhsoan.port.outbound.repository.patient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.patient.PatientConsentRecord;

public interface PatientConsentHistoryRepository {

    PatientConsentRecord save(PatientConsentRecord record);

    List<PatientConsentRecord> findByPatientId(UUID patientId);

    Optional<PatientConsentRecord> findLatestByPatientId(UUID patientId);

    /**
     * Lấy số phiên bản tiếp theo cho lịch sử phiếu đồng ý của bệnh nhân.
     * <p>
     * <b>Concurrency Invariant:</b> Caller bắt buộc phải nắm giữ khóa bi quan độc quyền
     * (Pessimistic Write Lock) trên Aggregate Root {@code Patient} thông qua
     * {@code patientRepository.findByIdForUpdate(patientId)} trong cùng transaction nghiệp vụ
     * trước khi gọi method này và lưu bản ghi mới để đảm bảo tính tuần tự hóa (serialized) tuyệt đối.
     * </p>
     *
     * @param patientId định danh của bệnh nhân
     * @return số phiên bản tiếp theo (bắt đầu từ 1 nếu chưa có lịch sử)
     */
    int getNextVersionNumber(UUID patientId);
}
