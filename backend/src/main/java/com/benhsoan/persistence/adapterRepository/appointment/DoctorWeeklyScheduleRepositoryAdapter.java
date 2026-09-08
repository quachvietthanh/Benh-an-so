package com.benhsoan.persistence.adapterRepository.appointment;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.appointment.DoctorWeeklySchedule;
import com.benhsoan.persistence.entity.appointment.DoctorWeeklyScheduleEntity;
import com.benhsoan.persistence.jpaRepository.appointment.JpaDoctorWeeklyScheduleRepository;
import com.benhsoan.persistence.mapper.appointment.DoctorWeeklySchedulePersistenceMapper;
import com.benhsoan.port.outbound.repository.appointment.DoctorWeeklyScheduleRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DoctorWeeklyScheduleRepositoryAdapter implements DoctorWeeklyScheduleRepository {

    private final JpaDoctorWeeklyScheduleRepository jpaRepository;
    private final DoctorWeeklySchedulePersistenceMapper mapper;

    @Override
    public List<DoctorWeeklySchedule> findByDoctorId(UUID doctorId) {
        return jpaRepository.findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(doctorId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<DoctorWeeklySchedule> findByDoctorIdAndDayOfWeek(UUID doctorId, DayOfWeek dayOfWeek) {
        return jpaRepository.findByDoctorIdAndDayOfWeek(doctorId, dayOfWeek.getValue()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public DoctorWeeklySchedule save(DoctorWeeklySchedule schedule) {
        DoctorWeeklyScheduleEntity entity = mapper.toEntity(schedule);
        DoctorWeeklyScheduleEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<DoctorWeeklySchedule> saveAll(List<DoctorWeeklySchedule> schedules) {
        List<DoctorWeeklyScheduleEntity> entities = schedules.stream()
                .map(mapper::toEntity)
                .toList();
        return jpaRepository.saveAll(entities).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteByDoctorId(UUID doctorId) {
        jpaRepository.deleteByDoctorId(doctorId);
    }

}
