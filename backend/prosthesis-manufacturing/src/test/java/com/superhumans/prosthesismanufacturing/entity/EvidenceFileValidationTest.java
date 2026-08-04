package com.superhumans.prosthesismanufacturing.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvidenceFileValidationTest {

    @Test
    void shouldRejectOversizedFile() {
        StepExecution execution = new StepExecution();
        EvidenceFile file = EvidenceFile.builder()
                .stepExecution(execution)
                .fileName("photo.jpg")
                .mimeType("image/jpeg")
                .sizeBytes(EvidenceFile.MAX_SIZE_BYTES + 1)
                .fileData(new byte[1])
                .build();

        assertThatThrownBy(file::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10 MB");
    }

    @Test
    void shouldRejectNonImageOrPdfMime() {
        StepExecution execution = new StepExecution();
        EvidenceFile file = EvidenceFile.builder()
                .stepExecution(execution)
                .fileName("script.exe")
                .mimeType("application/x-msdownload")
                .sizeBytes(1024L)
                .fileData(new byte[1])
                .build();

        assertThatThrownBy(file::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("image");
    }

    @Test
    void shouldAcceptImageAndPdf() {
        StepExecution execution = new StepExecution();
        EvidenceFile image = EvidenceFile.builder()
                .stepExecution(execution)
                .fileName("photo.png")
                .mimeType("image/png")
                .sizeBytes(2048L)
                .fileData(new byte[1])
                .build();
        EvidenceFile pdf = EvidenceFile.builder()
                .stepExecution(execution)
                .fileName("report.pdf")
                .mimeType("application/pdf")
                .sizeBytes(4096L)
                .fileData(new byte[1])
                .build();

        assertThatCode(image::validate).doesNotThrowAnyException();
        assertThatCode(pdf::validate).doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptExactlyTenMb() {
        StepExecution execution = new StepExecution();
        EvidenceFile file = EvidenceFile.builder()
                .stepExecution(execution)
                .fileName("big.pdf")
                .mimeType("application/pdf")
                .sizeBytes(EvidenceFile.MAX_SIZE_BYTES)
                .fileData(new byte[1])
                .build();

        assertThatCode(file::validate).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectBlankMime() {
        StepExecution execution = new StepExecution();
        EvidenceFile file = EvidenceFile.builder()
                .stepExecution(execution)
                .fileName("photo.png")
                .mimeType("  ")
                .sizeBytes(1024L)
                .fileData(new byte[1])
                .build();

        assertThatThrownBy(file::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MIME");
    }
}
