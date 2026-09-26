package com.benhsoan.persistence.adapterRepository.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.benhsoan.domain.appointment.AppointmentWaitlist;
import com.benhsoan.domain.appointment.enums.TimePreference;
import com.benhsoan.domain.appointment.exception.PatientAlreadyInWaitlistException;
import com.benhsoan.persistence.entity.appointment.AppointmentWaitlistEntity;
import com.benhsoan.persistence.jpaRepository.appointment.JpaAppointmentWaitlistRepository;
import com.benhsoan.persistence.mapper.appointment.AppointmentWaitlistPersistenceMapper;

class AppointmentWaitlistRepositoryAdapterTest {

    private JpaAppointmentWaitlistRepository jpaRepository;
    private AppointmentWaitlistPersistenceMapper mapper;
    private AppointmentWaitlistRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        jpaRepository = mock(JpaAppointmentWaitlistRepository.class);
        mapper = new AppointmentWaitlistPersistenceMapper();
        adapter = new AppointmentWaitlistRepositoryAdapter(jpaRepository, mapper);
    }

    @Test
    @DisplayName("Lưu thành công khi không có xung đột dữ liệu")
    void shouldSaveSuccessfully() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 11, 20);

        AppointmentWaitlist waitlist = AppointmentWaitlist.create(
                patientId, doctorId, date, TimePreference.ANYTIME, "Chờ khám", createdBy, Instant.now()
        );

        when(jpaRepository.saveAndFlush(any(AppointmentWaitlistEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AppointmentWaitlist saved = adapter.save(waitlist);

        assertThat(saved).isNotNull();
        assertThat(saved.getPatientId()).isEqualTo(patientId);
        verify(jpaRepository).saveAndFlush(any(AppointmentWaitlistEntity.class));
    }

    @Test
    @DisplayName("Bắt DataIntegrityViolationException và ném PatientAlreadyInWaitlistException khi trạng thái WAITING bị trùng lặp ở DB")
    void shouldTranslateDataIntegrityViolationExceptionToPatientAlreadyInWaitlistException() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 11, 20);

        AppointmentWaitlist waitlist = AppointmentWaitlist.create(
                patientId, doctorId, date, TimePreference.ANYTIME, "Chờ khám", createdBy, Instant.now()
        );

        when(jpaRepository.saveAndFlush(any(AppointmentWaitlistEntity.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for key uq_waitlist_patient_doctor_date_status"));

        assertThatThrownBy(() -> adapter.save(waitlist))
                .isInstanceOf(PatientAlreadyInWaitlistException.class)
                .hasMessageContaining("đã có tên trong danh sách chờ");
    }
}
