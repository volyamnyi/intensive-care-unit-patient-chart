package com.superhumans.prosthesismanufacturing.service;

import com.superhumans.exception.BadRequestException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.prosthesismanufacturing.dto.EvidenceFileResponse;
import com.superhumans.prosthesismanufacturing.entity.EvidenceFile;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.StepExecution;
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

    @Transactional
    public EvidenceFileResponse upload(UUID instanceId, UUID executionId, MultipartFile file, Long userId) {
        FlowInstance instance = instanceService.requireOwner(instanceId, userId);
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
        String mimeType = resolveMimeType(file);
        EvidenceFile evidence = EvidenceFile.builder()
                .stepExecution(execution)
                .fileName(file.getOriginalFilename())
                .mimeType(mimeType)
                .sizeBytes(file.getSize())
                .checksum(checksum(file))
                .fileData(toBytes(file))
                .build();
        evidenceFileRepository.save(evidence);
        auditService.logAction("EvidenceFile", evidence.getId(), "UPLOAD", userId);
        return toResponse(evidence);
    }

    @Transactional(readOnly = true)
    public EvidenceFile download(UUID fileId, Long userId, boolean allowAll) {
        EvidenceFile evidence = evidenceFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("Evidence file not found: " + fileId));
        return evidence;
    }

    private String resolveMimeType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && (contentType.startsWith("image/")
                || "application/pdf".equals(contentType))) {
            return contentType.toLowerCase(Locale.ROOT);
        }
        String name = file.getOriginalFilename();
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

    private String checksum(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(file.getBytes()));
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
