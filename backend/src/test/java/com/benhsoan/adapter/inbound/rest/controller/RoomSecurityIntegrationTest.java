package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.RoomRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.port.dto.result.RoomResult;
import com.benhsoan.port.inbound.queue.ActivateRoomUseCase;
import com.benhsoan.port.inbound.queue.CreateRoomUseCase;
import com.benhsoan.port.inbound.queue.DeactivateRoomUseCase;
import com.benhsoan.port.inbound.queue.GetRoomUseCase;
import com.benhsoan.port.inbound.queue.SearchRoomsUseCase;
import com.benhsoan.port.inbound.queue.UpdateRoomUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = RoomController.class)
@Import({RoomRestMapper.class, SecurityConfig.class, JwtAuthenticationFilter.class})
class RoomSecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private SearchRoomsUseCase searchRoomsUseCase;
    @MockitoBean private GetRoomUseCase getRoomUseCase;
    @MockitoBean private CreateRoomUseCase createRoomUseCase;
    @MockitoBean private UpdateRoomUseCase updateRoomUseCase;
    @MockitoBean private ActivateRoomUseCase activateRoomUseCase;
    @MockitoBean private DeactivateRoomUseCase deactivateRoomUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private ClockPort clockPort;

    @Test
    void allowsAllOperationalRolesToReadRooms() throws Exception {
        when(searchRoomsUseCase.search(any())).thenReturn(Page.empty());

        for (String role : new String[] {"ADMIN", "DOCTOR", "NURSE", "RECEPTIONIST"}) {
            mockMvc.perform(get("/rooms").with(user("tester").roles(role)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void onlyAllowsAdminToManageRooms() throws Exception {
        String requestBody = "{\"code\":\"P106\",\"name\":\"Phong kham 106\"}";
        Instant now = Instant.parse("2026-08-02T02:00:00Z");
        when(createRoomUseCase.create(any())).thenReturn(
                new RoomResult(UUID.randomUUID(), "P106", "Phong kham 106", true, now, null));

        mockMvc.perform(post("/rooms")
                        .with(user("receptionist").roles("RECEPTIONIST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/rooms")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());
    }
}
