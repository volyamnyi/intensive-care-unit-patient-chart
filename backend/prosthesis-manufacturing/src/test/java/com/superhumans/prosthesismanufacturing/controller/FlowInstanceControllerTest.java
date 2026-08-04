package com.superhumans.prosthesismanufacturing.controller;

import com.superhumans.prosthesismanufacturing.dto.EvidenceFileResponse;
import com.superhumans.prosthesismanufacturing.dto.FlowInstanceResponse;
import com.superhumans.prosthesismanufacturing.entity.EvidenceFile;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.prosthesismanufacturing.service.EvidenceFileService;
import com.superhumans.prosthesismanufacturing.service.FlowInstanceService;
import com.superhumans.prosthesismanufacturing.service.QualityGateService;
import com.superhumans.config.EnableTestExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FlowInstanceController.class)
@EnableTestExceptionHandler
@Import(CurrentUser.class)
@AutoConfigureMockMvc(addFilters = false)
class FlowInstanceControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    FlowInstanceService instanceService;
    @MockitoBean
    QualityGateService gateService;
    @MockitoBean
    EvidenceFileService evidenceFileService;

    UUID instanceId = UUID.randomUUID();
    UUID executionId = UUID.randomUUID();
    UUID gateId = UUID.randomUUID();
    UUID fileId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(instanceService.create(any(), eq(1L))).thenReturn(response());
        when(instanceService.list(any(), any())).thenReturn(List.of(response()));
        when(instanceService.get(eq(instanceId), eq(1L), eq(false))).thenReturn(response());
        when(instanceService.getSnapshot(eq(instanceId), eq(1L), eq(false))).thenReturn(Map.of("stage", "check"));
        when(instanceService.start(eq(instanceId), eq(1L))).thenReturn(response());
        when(instanceService.completeStep(eq(instanceId), eq(executionId), any(), eq(1L))).thenReturn(response());
        when(instanceService.pause(eq(instanceId), any(), eq(1L))).thenReturn(response());
        when(instanceService.resume(eq(instanceId), eq(1L))).thenReturn(response());
        when(instanceService.fail(eq(instanceId), any(), any(), any(), eq(1L))).thenReturn(response());
        when(instanceService.replacement(eq(instanceId), eq(1L))).thenReturn(response());
        when(gateService.decide(eq(instanceId), eq(gateId), any(), eq(1L), eq(false))).thenReturn(response());
        when(evidenceFileService.upload(eq(instanceId), eq(executionId), any(), eq(1L))).thenReturn(
                EvidenceFileResponse.builder().id(fileId).stepExecutionId(executionId).fileName("photo.png").mimeType("image/png").sizeBytes(3L).checksum("abc123").build());
        when(evidenceFileService.download(eq(fileId), eq(1L), eq(false))).thenReturn(
                EvidenceFile.builder().fileName("photo.png").mimeType("image/png").sizeBytes(3L).fileData(new byte[]{1, 2, 3}).build());
        when(instanceService.generateReport(eq(instanceId), eq(1L), eq(false))).thenReturn(new byte[]{0x25, 0x50, 0x44, 0x46});
    }

    @Test
    void create_returnsCreated() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/prosthesis-manufacturing/instances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated());
    }

    @Test
    void list_returnsOk() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/prosthesis-manufacturing/instances"))
                .andExpect(status().isOk());
    }

    @Test
    void get_returnsOk() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/prosthesis-manufacturing/instances/{id}", instanceId))
                .andExpect(status().isOk());
    }

    @Test
    void snapshot_returnsOk() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/prosthesis-manufacturing/instances/{id}/snapshot", instanceId))
                .andExpect(status().isOk());
    }

    @Test
    void start_returnsOk() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/prosthesis-manufacturing/instances/{id}/start", instanceId))
                .andExpect(status().isOk());
    }

    @Test
    void completeStep_returnsOk() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(
                        "/api/prosthesis-manufacturing/instances/{id}/steps/{executionId}/complete",
                        instanceId, executionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void pause_returnsOk() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(
                        "/api/prosthesis-manufacturing/instances/{id}/pause", instanceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void resume_returnsOk() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(
                        "/api/prosthesis-manufacturing/instances/{id}/resume", instanceId))
                .andExpect(status().isOk());
    }

    @Test
    void fail_returnsOk() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(
                        "/api/prosthesis-manufacturing/instances/{id}/fail", instanceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void replacement_returnsCreated() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(
                        "/api/prosthesis-manufacturing/instances/{id}/replacement", instanceId))
                .andExpect(status().isCreated());
    }

    @Test
    void gateDecision_returnsOk() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(
                        "/api/prosthesis-manufacturing/instances/{id}/gates/{gateId}/decision",
                        instanceId, gateId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void uploadEvidence_returnsCreated() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart(
                        "/api/prosthesis-manufacturing/instances/{id}/evidence", instanceId)
                        .param("executionId", executionId.toString())
                        .file(new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2, 3})))
                .andExpect(status().isCreated());
    }

    @Test
    void downloadEvidence_returnsOk() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(
                        "/api/prosthesis-manufacturing/instances/{id}/evidence/{fileId}",
                        instanceId, fileId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"photo.png\""))
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    void generateReport_returnsPdf() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(
                        "/api/prosthesis-manufacturing/instances/{id}/pdf", instanceId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"report_" + instanceId + ".pdf\""));
    }

    private FlowInstanceResponse response() {
        return FlowInstanceResponse.builder().id(instanceId).status(FlowInstanceStatus.NEW.name()).build();
    }
}