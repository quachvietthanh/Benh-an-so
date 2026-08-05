package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.RoomRestMapper;
import com.benhsoan.port.dto.command.queue.SearchRoomsQuery;
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
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = RoomController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RoomRestMapper.class)
class RoomControllerTest {

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
    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private ClockPort clockPort;

    @Test
    void searchesRoomsAsPage() throws Exception {
        when(searchRoomsUseCase.search(any())).thenReturn(new PageImpl<>(List.of(result(true))));

        mockMvc.perform(get("/rooms").param("keyword", "P1").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("P103"))
                .andExpect(jsonPath("$.content[0].active").value(true));
    }

    @Test
    void searchesActiveRoomsByDefault() throws Exception {
        when(searchRoomsUseCase.search(any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/rooms"))
                .andExpect(status().isOk());

        ArgumentCaptor<SearchRoomsQuery> queryCaptor = ArgumentCaptor.forClass(SearchRoomsQuery.class);
        verify(searchRoomsUseCase).search(queryCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(true, queryCaptor.getValue().active());
    }

    @Test
    void createsRoom() throws Exception {
        when(createRoomUseCase.create(any())).thenReturn(result(true));

        mockMvc.perform(post("/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"P103","name":"Phong kham 103"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("P103"));
    }

    @Test
    void rejectsInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":" ","name":" "}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createRoomUseCase);
    }

    @Test
    void updatesAndChangesRoomStatus() throws Exception {
        UUID roomId = UUID.randomUUID();
        when(updateRoomUseCase.update(any())).thenReturn(result(true));
        when(deactivateRoomUseCase.deactivate(roomId)).thenReturn(result(false));
        when(activateRoomUseCase.activate(roomId)).thenReturn(result(true));

        mockMvc.perform(put("/rooms/{roomId}", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Phong kham Noi"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/rooms/{roomId}/deactivate", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(patch("/rooms/{roomId}/activate", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    private RoomResult result(boolean active) {
        Instant now = Instant.parse("2026-08-02T02:00:00Z");
        return new RoomResult(UUID.randomUUID(), "P103", "Phong kham 103", active, now, null);
    }
}
