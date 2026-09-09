package com.benhsoan.persistence.adapterRepository.appointment;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.appointment.DoctorWeeklySchedule;
import com.benhsoan.persistence.jpaRepository.appointment.JpaDoctorWeeklyScheduleRepository;
import com.benhsoan.persistence.mapper.appointment.DoctorWeeklySchedulePersistenceMapper;
import com.benhsoan.port.outbound.repository.appointment.DoctorWeeklyScheduleRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class DoctorWeeklyScheduleRepositoryAdapter implements DoctorWeeklyScheduleRepository {

    private final JpaDoctorWeeklyScheduleRepository jpaRepository;
    private final DoctorWeeklySchedulePersistenceMapper mapper;

    @Override
    public List<DoctorWeeklySchedule> findByDoctorId(UUID doctorId) {
        return jpaRepository.findByDoctorId(doctorId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<DoctorWeeklySchedule> findActiveByDoctorId(UUID doctorId) {
        return jpaRepository.findByDoctorIdAndActiveTrue(doctorId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<DoctorWeeklySchedule> findByDoctorIdAndDayOfWeek(UUID doctorId, DayOfWeek dayOfWeek) {
        return jpaRepository.findByDoctorIdAndDayOfWeek(doctorId, dayOfWeek)
                .map(mapper::toDomain);
    }

    @Override
    public DoctorWeeklySchedule save(DoctorWeeklySchedule schedule) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(schedule)));
    }

    @Override
    public List<DoctorWeeklySchedule> saveAll(List<DoctorWeeklySchedule> schedules) {
        var entities = schedules.stream().map(mapper::toEntity).toList();
        return jpaRepository.saveAll(entities).stream().map(mapper::toDomain).toList();
    }
}
