package com.benhsoan.application.ucservice.queue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.queue.MedicalQueue;
import com.benhsoan.domain.queue.enums.PriorityLevel;
import com.benhsoan.persistence.entity.appointment.AppointmentEntity;
import com.benhsoan.persistence.jpaRepository.appointment.AppointmentBusinessSpecification;
import com.benhsoan.persistence.jpaRepository.appointment.JpaAppointmentRepository;
import com.benhsoan.port.dto.command.queue.AddToQueueCommand;
import com.benhsoan.port.dto.result.QueueResult;
import com.benhsoan.port.inbound.queue.AddToQueueUseCase;
import com.benhsoan.port.outbound.repository.crudRepository.queue.MedicalQueueRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class AddToQueueService implements AddToQueueUseCase {

    private final MedicalQueueRepository medicalQueueRepository;

    private final JpaAppointmentRepository jpaAppointmentRepository;

    private static final int MAX_RETRIES = 3;

    @Override
    public QueueResult addToQueue(AddToQueueCommand command) {

        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return doAddToQueue(command);
            } catch (DataIntegrityViolationException e) {
                lastException = e;
                if (attempt == MAX_RETRIES) {
                    throw e;
                }
            }
        }

        throw lastException;
    }

    private QueueResult doAddToQueue(AddToQueueCommand command) {

        Instant startOfDay = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        Instant endOfDay = LocalDate.now()
                .atTime(LocalTime.MAX)
                .atZone(ZoneId.systemDefault())
                .toInstant();

        // Auto-detect APPOINTMENT priority if patient has an appointment today
        PriorityLevel effectivePriority = resolvePriority(command, startOfDay, endOfDay);

        int nextNumber = medicalQueueRepository
                .findMaxQueueNumberForToday(
                        command.roomNumber(),
                        startOfDay,
                        endOfDay
                ) + 1;

        MedicalQueue queue = MedicalQueue.create(
                command.patientId(),
                nextNumber,
                effectivePriority,
                command.roomNumber(),
                command.doctorId(),
                command.createdBy()
        );

        MedicalQueue saved = medicalQueueRepository.save(queue);

        return QueueResult.from(saved);
    }

    private PriorityLevel resolvePriority(AddToQueueCommand command,
                                          Instant startOfDay,
                                          Instant endOfDay) {
        // If already EMERGENCY, keep it
        if (command.priorityLevel() == PriorityLevel.EMERGENCY) {
            return PriorityLevel.EMERGENCY;
        }

        // Disambiguate findOne by casting to JpaSpecificationExecutor
        JpaSpecificationExecutor<AppointmentEntity> specExecutor =
                (JpaSpecificationExecutor<AppointmentEntity>) jpaAppointmentRepository;

        Specification<AppointmentEntity> spec = AppointmentBusinessSpecification
                .hasPatient(command.patientId())
                .and(AppointmentBusinessSpecification.today(startOfDay, endOfDay))
                .and(AppointmentBusinessSpecification.active());

        boolean hasAppointmentToday = specExecutor.findOne(spec).isPresent();

        if (hasAppointmentToday) {
            return PriorityLevel.APPOINTMENT;
        }

        return command.priorityLevel();
    }
}
