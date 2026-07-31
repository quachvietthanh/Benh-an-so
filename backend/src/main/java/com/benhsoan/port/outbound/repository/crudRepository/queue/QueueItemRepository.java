package com.benhsoan.port.outbound.repository.crudRepository.queue;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.queue.QueueItem;

public interface QueueItemRepository { Optional<QueueItem> findByAppointmentId(UUID appointmentId); boolean existsByPatientIdAndQueueDate(UUID patientId, LocalDate queueDate); }
