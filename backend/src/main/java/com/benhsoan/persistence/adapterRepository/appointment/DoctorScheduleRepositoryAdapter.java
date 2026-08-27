package com.benhsoan.persistence.adapterRepository.appointment;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.appointment.DoctorSchedule;
import com.benhsoan.persistence.jpaRepository.appointment.JpaDoctorScheduleRepository;
import com.benhsoan.persistence.mapper.appointment.DoctorSchedulePersistenceMapper;
import com.benhsoan.port.outbound.repository.appointment.DoctorScheduleRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class DoctorScheduleRepositoryAdapter implements DoctorScheduleRepository {

    private final JpaDoctorScheduleRepository jpaRepository;

    private final DoctorSchedulePersistenceMapper mapper;

    @Override
    public Optional<DoctorSchedule> findByDoctorIdAndScheduleDate(UUID doctorId, LocalDate scheduleDate) {
        return jpaRepository.findByDoctorIdAndScheduleDate(doctorId, scheduleDate)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<DoctorSchedule> findByDoctorIdAndScheduleDateForUpdate(UUID doctorId, LocalDate scheduleDate) {
        return jpaRepository.findByDoctorIdAndScheduleDateForUpdate(doctorId, scheduleDate)
                .map(mapper::toDomain);
    }

    @Override
    public DoctorSchedule save(DoctorSchedule schedule) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(schedule)));
    }

}
