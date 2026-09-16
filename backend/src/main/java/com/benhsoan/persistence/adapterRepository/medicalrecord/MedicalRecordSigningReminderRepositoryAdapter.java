package com.benhsoan.persistence.adapterRepository.medicalrecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.medicalrecord.MedicalRecordSigningReminder;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordSigningReminderRepository;
import com.benhsoan.persistence.mapper.medicalrecord.MedicalRecordSigningReminderPersistenceMapper;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordSigningReminderRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MedicalRecordSigningReminderRepositoryAdapter
        implements MedicalRecordSigningReminderRepository {

    private final JpaMedicalRecordSigningReminderRepository jpaRepository;
    private final MedicalRecordSigningReminderPersistenceMapper mapper;

    @Override
    public MedicalRecordSigningReminder save(MedicalRecordSigningReminder reminder) {
        var entity = mapper.toEntity(reminder);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<MedicalRecordSigningReminder> findByMedicalRecordId(UUID medicalRecordId) {
        return jpaRepository.findByMedicalRecordIdOrderByRemindedAtDesc(medicalRecordId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countByMedicalRecordId(UUID medicalRecordId) {
        return jpaRepository.countByMedicalRecordId(medicalRecordId);
    }

    @Override
    public Optional<MedicalRecordSigningReminder> findLatestByMedicalRecordId(UUID medicalRecordId) {
        return jpaRepository.findTopByMedicalRecordIdOrderByRemindedAtDesc(medicalRecordId)
                .map(mapper::toDomain);
    }
}
