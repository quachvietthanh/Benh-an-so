package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import com.benhsoan.adapter.inbound.rest.mapper.ClinicalResultAttachmentRestMapper;
import com.benhsoan.domain.clinical.enums.MedicalAttachmentType;
import com.benhsoan.port.dto.result.ClinicalAttachmentDownloadResult;
import com.benhsoan.port.dto.result.ClinicalResultResult;
import com.benhsoan.port.inbound.clinical.DownloadClinicalResultAttachmentUseCase;
import com.benhsoan.port.inbound.clinical.UploadClinicalResultAttachmentUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = ClinicalResultAttachmentController.class,
        properties = "clinical-attachments.cloudinary.enabled=true")
@AutoConfigureMockMvc(addFilters = false)
@Import(ClinicalResultAttachmentRestMapper.class)
class ClinicalResultAttachmentControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UploadClinicalResultAttachmentUseCase uploadClinicalResultAttachmentUseCase;
    @MockitoBean private DownloadClinicalResultAttachmentUseCase downloadClinicalResultAttachmentUseCase;
    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private ClockPort clockPort;

    @Test
    void uploadsMultipartAttachment() throws Exception {
        UUID resultId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        when(uploadClinicalResultAttachmentUseCase.upload(eq(resultId), any())).thenReturn(
                new ClinicalResultResult.Attachment(attachmentId, "result.pdf", "application/pdf", 12,
                        MedicalAttachmentType.LAB_RESULT));
        MockMultipartFile file = new MockMultipartFile("file", "result.pdf", "application/pdf",
                "%PDF-1.7\nbody".getBytes());

        mockMvc.perform(multipart("/clinical-results/{resultId}/attachments", resultId).file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(attachmentId.toString()))
                .andExpect(jsonPath("$.contentType").value("application/pdf"));
    }

    @Test
    void rejectsMissingMultipartFile() throws Exception {
        mockMvc.perform(multipart("/clinical-results/{resultId}/attachments", UUID.randomUUID()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsFileWithoutContentType() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "result.pdf", null, "%PDF-1.7".getBytes());

        mockMvc.perform(multipart("/clinical-results/{resultId}/attachments", UUID.randomUUID()).file(file))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(uploadClinicalResultAttachmentUseCase);
    }

    @Test
    void returnsSignedDownloadUrl() throws Exception {
        UUID attachmentId = UUID.randomUUID();
        when(downloadClinicalResultAttachmentUseCase.createDownloadUrl(attachmentId)).thenReturn(
                new ClinicalAttachmentDownloadResult(attachmentId, "https://cloudinary.example/signed",
                        Instant.parse("2026-08-20T01:05:00Z")));

        mockMvc.perform(get("/clinical-result-attachments/{attachmentId}/download", attachmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attachmentId").value(attachmentId.toString()))
                .andExpect(jsonPath("$.url").value("https://cloudinary.example/signed"));
    }
}
