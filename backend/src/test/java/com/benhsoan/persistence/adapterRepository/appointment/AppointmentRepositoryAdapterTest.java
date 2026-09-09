package com.benhsoan.persistence.adapterRepository.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.persistence.entity.appointment.AppointmentEntity;
import com.benhsoan.persistence.jpaRepository.appointment.JpaAppointmentRepository;
import com.benhsoan.persistence.mapper.appointment.AppointmentPersistenceMapper;

@ExtendWith(MockitoExtension.class)
class AppointmentRepositoryAdapterTest {

    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final Instant FROM = Instant.parse("2026-09-14T08:00:00Z");
    private static final Instant TO = Instant.parse("2026-09-14T09:00:00Z");

    @Mock private JpaAppointmentRepository jpaRepository;
    @Mock private AppointmentPersistenceMapper mapper;

    private AppointmentRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AppointmentRepositoryAdapter(jpaRepository, mapper);
    }

    @Test
    void findActiveAppointmentsForDoctorBetweenPassesActiveStatuses() {
        AppointmentEntity entity = new AppointmentEntity();
        Appointment domain = org.mockito.Mockito.mock(Appointment.class);
        when(domain.getStatus()).thenReturn(AppointmentStatus.CONFIRMED);

        when(jpaRepository.findActiveForDoctorBetween(DOCTOR_ID, FROM, TO, AppointmentStatus.ACTIVE_STATUSES))
                .thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<Appointment> results = adapter.findActiveAppointmentsForDoctorBetween(DOCTOR_ID, FROM, TO);

        assertEquals(1, results.size());
        assertEquals(AppointmentStatus.CONFIRMED, results.get(0).getStatus());
        verify(jpaRepository).findActiveForDoctorBetween(DOCTOR_ID, FROM, TO, AppointmentStatus.ACTIVE_STATUSES);
    }

    @Test
    void existsActiveAppointmentConflictQueriesWithActiveSpecification() {
        when(jpaRepository.exists(any(Specification.class))).thenReturn(true);

        boolean exists = adapter.existsActiveAppointmentConflict(DOCTOR_ID, FROM, TO);

        assertTrue(exists);
        verify(jpaRepository).exists(any(Specification.class));
    }
}
