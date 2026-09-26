package com.benhsoan.application.ucservice.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.queue.enums.MedicalQueueStatus;
import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.persistence.adapterRepository.appointment.AppointmentRepositoryAdapter;
import com.benhsoan.persistence.adapterRepository.queue.MedicalQueueRepositoryAdapter;
import com.benhsoan.persistence.adapterRepository.queue.QueueItemRepositoryAdapter;
import com.benhsoan.persistence.adapterRepository.visit.VisitRepositoryAdapter;
import com.benhsoan.persistence.entity.appointment.AppointmentEntity;
import com.benhsoan.persistence.entity.queue.MedicalQueueEntity;
import com.benhsoan.persistence.entity.queue.QueueItemEntity;
import com.benhsoan.persistence.entity.visit.VisitEntity;
import com.benhsoan.persistence.jpaRepository.appointment.JpaAppointmentRepository;
import com.benhsoan.persistence.jpaRepository.queue.JpaMedicalQueueRepository;
import com.benhsoan.persistence.jpaRepository.queue.JpaQueueItemRepository;
import com.benhsoan.persistence.jpaRepository.visit.JpaVisitRepository;
import com.benhsoan.persistence.mapper.appointment.AppointmentPersistenceMapper;
import com.benhsoan.persistence.mapper.queue.MedicalQueuePersistenceMapper;
import com.benhsoan.persistence.mapper.queue.QueueStructurePersistenceMapper;
import com.benhsoan.persistence.mapper.visit.VisitPersistenceMapper;
import com.benhsoan.port.dto.command.queue.SkipQueueItemCommand;
import com.benhsoan.port.outbound.repository.queue.QueueItemQueryRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({
        SkipQueueItemService.class,
        QueueOperationAuthorization.class,
        QueueItemRepositoryAdapter.class,
        MedicalQueueRepositoryAdapter.class,
        VisitRepositoryAdapter.class,
        AppointmentRepositoryAdapter.class,
        QueueStructurePersistenceMapper.class,
        MedicalQueuePersistenceMapper.class,
        VisitPersistenceMapper.class,
        AppointmentPersistenceMapper.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SkipQueueItemTransactionIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-02T02:00:00Z");

    @Autowired private SkipQueueItemService service;
    @Autowired private JpaQueueItemRepository queueItemRepository;
    @Autowired private JpaMedicalQueueRepository medicalQueueRepository;
    @Autowired private JpaVisitRepository visitRepository;
    @Autowired private JpaAppointmentRepository appointmentRepository;

    @MockitoSpyBean private AppointmentRepositoryAdapter appointmentRepositoryAdapter;
    @MockitoBean private QueueItemQueryRepository queueItemQueryRepository;
    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private QueueAuditService queueAuditService;

    @Test
    void rollsBackQueueItemAndVisitWhenAppointmentSaveFails() {
        UUID doctorId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID queueId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        medicalQueueRepository.save(MedicalQueueEntity.builder()
                .id(queueId).doctorId(doctorId).roomId(UUID.randomUUID()).queueDate(LocalDate.of(2026, 8, 2))
                .status(MedicalQueueStatus.OPEN).createdAt(NOW).updatedAt(NOW).build());
        appointmentRepository.save(AppointmentEntity.builder()
                .id(appointmentId).appointmentCode("AP900001").patientId(patientId).doctorId(doctorId)
                .startTime(NOW.plusSeconds(3600)).endTime(NOW.plusSeconds(5400))
                .status(AppointmentStatus.IN_PROGRESS).reason("Consultation").checkedInAt(NOW)
                .createdBy(actorId).createdAt(NOW).build());
        visitRepository.save(VisitEntity.builder()
                .id(visitId).visitCode("VIS900001").patientId(patientId).doctorId(doctorId)
                .appointmentId(appointmentId).queueItemId(itemId).visitType(VisitType.APPOINTMENT)
                .status(VisitStatus.IN_PROGRESS).visitAt(NOW).startedAt(NOW.plusSeconds(30))
                .reason("Consultation").createdBy(actorId).createdAt(NOW).updatedAt(NOW.plusSeconds(30)).build());
        QueueItemEntity item = new QueueItemEntity();
        item.setId(itemId);
        item.setMedicalQueueId(queueId);
        item.setPatientId(patientId);
        item.setAppointmentId(appointmentId);
        item.setVisitId(visitId);
        item.setSourceType(QueueItemSourceType.APPOINTMENT);
        item.setStatus(QueueItemStatus.IN_PROGRESS);
        item.setQueueNumber(1);
        item.setQueueDate(LocalDate.of(2026, 8, 2));
        item.setCheckedInAt(NOW);
        item.setCalledAt(NOW.plusSeconds(30));
        item.setCreatedBy(actorId);
        item.setCreatedAt(NOW);
        item.setUpdatedAt(NOW.plusSeconds(30));
        queueItemRepository.save(item);

        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(clockPort.now()).thenReturn(NOW.plusSeconds(60));
        doThrow(new IllegalStateException("Simulated appointment persistence failure"))
                .when(appointmentRepositoryAdapter).save(any(Appointment.class));

        assertThrows(IllegalStateException.class,
                () -> service.skip(new SkipQueueItemCommand(itemId, "Patient absent when called")));

        assertEquals(QueueItemStatus.IN_PROGRESS, queueItemRepository.findById(itemId).orElseThrow().getStatus());
        assertEquals(VisitStatus.IN_PROGRESS, visitRepository.findById(visitId).orElseThrow().getStatus());
        assertEquals(AppointmentStatus.IN_PROGRESS,
                appointmentRepository.findById(appointmentId).orElseThrow().getStatus());
    }
}
