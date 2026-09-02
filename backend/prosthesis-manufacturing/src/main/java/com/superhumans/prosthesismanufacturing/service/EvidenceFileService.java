package com.superhumans.prosthesismanufacturing.service;

import com.superhumans.exception.BadRequestException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.prosthesismanufacturing.dto.EvidenceFileResponse;
import com.superhumans.prosthesismanufacturing.entity.EvidenceFile;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.prosthesismanufacturing.entity.StepExecution;
import com.superhumans.prosthesismanufacturing.entity.StepExecutionStatus;
import com.superhumans.prosthesismanufacturing.repository.EvidenceFileRepository;
import com.superhumans.prosthesismanufacturing.repository.StepExecutionRepository;
import com.superhumans.service.AuditService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EvidenceFileService {

    EvidenceFileRepository evidenceFileRepository;
    StepExecutionRepository executionRepository;
    FlowInstanceService instanceService;
    AuditService auditService;

    private static final int MAX_FILES_PER_EXECUTION = 10;

    @Transactional
    public EvidenceFileResponse upload(UUID instanceId, UUID executionId, MultipartFile file, Long userId) {
        FlowInstance instance = instanceService.requireOwner(instanceId, userId);
        if (instance.getStatus() != FlowInstanceStatus.IN_PROGRESS
                && instance.getStatus() != FlowInstanceStatus.PAUSED) {
            throw new BadRequestException("Файли можна завантажувати лише під час виконання процесу");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        if (file.getSize() > EvidenceFile.MAX_SIZE_BYTES) {
            throw new BadRequestException("File size must not exceed 10 MB");
        }
        StepExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new NotFoundException("Execution not found: " + executionId));
        if (!instanceId.equals(execution.getInstance().getId())) {
            throw new BadRequestException("Execution does not belong to this instance");
        }
        if (execution.getStatus() != StepExecutionStatus.IN_PROGRESS) {
            throw new BadRequestException("Файли можна завантажувати лише на активному кроці");
        }
        long existingCount = evidenceFileRepository.findByStepExecutionId(executionId).size();
        if (existingCount >= MAX_FILES_PER_EXECUTION) {
            throw new BadRequestException("Досягнуто ліміт файлів на кроці (10)");
        }
        String mimeType = resolveMimeType(file);
        byte[] data = toBytes(file);
        validateContentMatchesType(data, mimeType);
        EvidenceFile evidence = EvidenceFile.builder()
                .stepExecution(execution)
                .fileName(sanitizeFileName(file.getOriginalFilename()))
                .mimeType(mimeType)
                .sizeBytes(file.getSize())
                .checksum(checksum(data))
                .fileData(data)
                .build();
        evidenceFileRepository.save(evidence);
        auditService.logAction("EvidenceFile", evidence.getId(), "UPLOAD", userId);
        return toResponse(evidence);
    }

    @Transactional(readOnly = true)
    public java.util.List<EvidenceFileResponse> listByExecution(UUID instanceId, UUID executionId, Long userId, boolean allowAll) {
        FlowInstance instance = instanceService.requireOwner(instanceId, userId, allowAll);
        StepExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new NotFoundException("Execution not found: " + executionId));
        if (!instanceId.equals(execution.getInstance().getId())) {
            throw new BadRequestException("Execution does not belong to this instance");
        }
        return evidenceFileRepository.findByStepExecutionId(executionId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(UUID instanceId, UUID fileId, Long userId) {
        FlowInstance instance = instanceService.requireOwner(instanceId, userId);
        if (instance.getStatus() != FlowInstanceStatus.IN_PROGRESS
                && instance.getStatus() != FlowInstanceStatus.PAUSED) {
            throw new BadRequestException("Файли можна видаляти лише під час виконання процесу");
        }
        EvidenceFile evidence = evidenceFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("Evidence file not found: " + fileId));
        if (!evidence.getStepExecution().getInstance().getId().equals(instanceId)) {
            throw new BadRequestException("File does not belong to this instance");
        }
        if (evidence.getStepExecution().getStatus() != StepExecutionStatus.IN_PROGRESS) {
            throw new BadRequestException("Файли можна видаляти лише на активному кроці");
        }
        evidenceFileRepository.delete(evidence);
        auditService.logAction("EvidenceFile", fileId, "DELETE", userId);
    }

    @Transactional(readOnly = true)
    public EvidenceFile download(UUID fileId, Long userId, boolean allowAll) {
        EvidenceFile evidence = evidenceFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("Evidence file not found: " + fileId));
        if (!allowAll && !evidence.getStepExecution().getInstance().getAssignedUserId().equals(userId)) {
            throw new NotFoundException("Evidence file not found: " + fileId);
        }
        return evidence;
    }

    private String resolveMimeType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("svg")) {
            throw new BadRequestException("SVG files are not allowed");
        }
        String name = file.getOriginalFilename();
        if (name != null && name.toLowerCase(Locale.ROOT).endsWith(".svg")) {
            throw new BadRequestException("SVG files are not allowed");
        }
        if (contentType != null && (contentType.startsWith("image/")
                || "application/pdf".equals(contentType))) {
            return contentType.toLowerCase(Locale.ROOT);
        }
        if (name != null) {
            String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            return switch (ext) {
                case "jpg", "jpeg" -> "image/jpeg";
                case "png" -> "image/png";
                case "gif" -> "image/gif";
                case "webp" -> "image/webp";
                case "bmp" -> "image/bmp";
                case "pdf" -> "application/pdf";
                default -> throw new BadRequestException("Only image and PDF files are allowed");
            };
        }
        throw new BadRequestException("Only image and PDF files are allowed");
    }

    private byte[] toBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception e) {
            throw new BadRequestException("File could not be read");
        }
    }

    /** Magic-byte sniffing: the declared type must match the actual content. */
    private void validateContentMatchesType(byte[] data, String mimeType) {
        if (!contentMatches(data, mimeType)) {
            throw new BadRequestException("File content does not match its declared type");
        }
    }

    private boolean contentMatches(byte[] data, String mimeType) {
        return switch (mimeType) {
            case "image/jpeg" -> startsWith(data, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            case "image/png" -> startsWith(data, new byte[]{(byte) 0x89, 'P', 'N', 'G'});
            case "image/gif" -> startsWith(data, new byte[]{'G', 'I', 'F', '8'});
            case "image/webp" -> data.length >= 12 && data[0] == 'R' && data[1] == 'I'
                    && data[2] == 'F' && data[3] == 'F' && data[8] == 'W' && data[9] == 'E'
                    && data[10] == 'B' && data[11] == 'P';
            case "image/bmp" -> startsWith(data, new byte[]{'B', 'M'});
            case "application/pdf" -> startsWith(data, new byte[]{'%', 'P', 'D', 'F'});
            default -> false;
        };
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    private String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) return "evidence";
        String cleaned = name.replaceAll("[\\r\\n\"\\\\/]", "_").trim();
        return cleaned.isBlank() ? "evidence" : cleaned;
    }

    private String checksum(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (Exception e) {
            throw new BadRequestException("File checksum could not be computed");
        }
    }

    private EvidenceFileResponse toResponse(EvidenceFile evidence) {
        return EvidenceFileResponse.builder()
                .id(evidence.getId())
                .stepExecutionId(evidence.getStepExecution().getId())
                .fileName(evidence.getFileName())
                .mimeType(evidence.getMimeType())
                .sizeBytes(evidence.getSizeBytes())
                .checksum(evidence.getChecksum())
                .createdAt(evidence.getCreatedAt())
                .build();
    }
}
