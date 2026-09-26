package com.benhsoan.application.ucservice.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.queue.Room;
import com.benhsoan.persistence.adapterRepository.queue.RoomRepositoryAdapter;
import com.benhsoan.persistence.jpaRepository.queue.JpaRoomRepository;
import com.benhsoan.persistence.mapper.queue.QueueStructurePersistenceMapper;
import com.benhsoan.port.dto.command.queue.CreateRoomCommand;
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
        CreateRoomService.class,
        RoomRepositoryAdapter.class,
        QueueStructurePersistenceMapper.class,
        RoomAuthorizationService.class,
        RoomResultMapper.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RoomTransactionIntegrationTest {

    @Autowired private CreateRoomService createRoomService;
    @Autowired private JpaRoomRepository roomRepository;

    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private RoomAuditService auditService;

    @Test
    void rollsBackRoomWhenAuditPersistenceFails() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(clockPort.now()).thenReturn(Instant.parse("2026-08-02T02:00:00Z"));
        doThrow(new IllegalStateException("Audit persistence failed"))
                .when(auditService).record(eq(ActionType.CREATE), any(Room.class));

        assertThrows(IllegalStateException.class,
                () -> createRoomService.create(new CreateRoomCommand("P105", "Phong kham 105")));

        assertEquals(0, roomRepository.count());
    }
}
