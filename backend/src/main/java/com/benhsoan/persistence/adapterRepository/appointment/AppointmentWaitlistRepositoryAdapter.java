package com.benhsoan.persistence.adapterRepository.appointment;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.benhsoan.domain.appointment.AppointmentWaitlist;
import com.benhsoan.domain.appointment.enums.WaitlistStatus;
import com.benhsoan.domain.appointment.exception.PatientAlreadyInWaitlistException;
import com.benhsoan.persistence.jpaRepository.appointment.JpaAppointmentWaitlistRepository;
import com.benhsoan.persistence.mapper.appointment.AppointmentWaitlistPersistenceMapper;
import com.benhsoan.port.outbound.repository.appointment.AppointmentWaitlistRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AppointmentWaitlistRepositoryAdapter implements AppointmentWaitlistRepository {

    private final JpaAppointmentWaitlistRepository jpaRepository;
    private final AppointmentWaitlistPersistenceMapper mapper;

    @Override
    public AppointmentWaitlist save(AppointmentWaitlist waitlist) {
        try {
            var entity = mapper.toEntity(waitlist);
            var saved = jpaRepository.saveAndFlush(entity);
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            if (waitlist.getStatus() == WaitlistStatus.WAITING) {
                throw new PatientAlreadyInWaitlistException(
                        "Bệnh nhân đã có tên trong danh sách chờ của bác sĩ vào ngày này."
                );
            }
            throw ex;
        }
    }


    @Override
    public Optional<AppointmentWaitlist> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<AppointmentWaitlist> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public Optional<AppointmentWaitlist> findFirstWaitingByDoctorAndDate(UUID doctorId, LocalDate desiredDate) {
        return jpaRepository.findFirstByDoctorIdAndDesiredDateAndStatusOrderByCreatedAtAsc(
                doctorId,
                desiredDate,
                WaitlistStatus.WAITING
        ).map(mapper::toDomain);
    }

    @Override
    public List<AppointmentWaitlist> findWaitlist(UUID doctorId, LocalDate desiredDate, LocalDate fromDate, WaitlistStatus status) {
        return jpaRepository.findWaitlist(doctorId, desiredDate, fromDate, status)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }


    @Override
    public boolean existsByPatientIdAndDoctorIdAndDesiredDateAndStatus(
            UUID patientId,
            UUID doctorId,
            LocalDate desiredDate,
            WaitlistStatus status
    ) {
        return jpaRepository.existsByPatientIdAndDoctorIdAndDesiredDateAndStatus(
                patientId,
                doctorId,
                desiredDate,
                status
        );
    }

    @Override
    public Optional<AppointmentWaitlist> findActiveByPatientAndDoctorAndDate(
            UUID patientId,
            UUID doctorId,
            LocalDate desiredDate
    ) {
        return jpaRepository.findFirstByPatientIdAndDoctorIdAndDesiredDateAndStatusOrderByCreatedAtAsc(
                patientId,
                doctorId,
                desiredDate,
                WaitlistStatus.WAITING
        ).map(mapper::toDomain);
    }
}
