package com.superhumans.prosthesismanufacturing.service;

import com.superhumans.exception.NotFoundException;
import com.superhumans.prosthesismanufacturing.dto.FailureSnapshotResponse;
import com.superhumans.prosthesismanufacturing.entity.FailureSnapshot;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.repository.FailureSnapshotRepository;
import com.superhumans.service.AuditService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FailureSnapshotService {

    FailureSnapshotRepository snapshotRepository;
    AuditService auditService;

    @Transactional
    public FailureSnapshot create(FlowInstance instance, String category, String description,
                                  String snapshotJson, Long userId) {
        FailureSnapshot snapshot = FailureSnapshot.builder()
                .instance(instance)
                .category(category)
                .description(description)
                .snapshot(snapshotJson)
                .build();
        snapshotRepository.save(snapshot);
        auditService.logAction("FailureSnapshot", snapshot.getId(), "CREATE", userId);
        return snapshot;
    }

    @Transactional(readOnly = true)
    public FailureSnapshotResponse getByInstance(UUID instanceId) {
        FailureSnapshot snapshot = snapshotRepository.findByInstanceId(instanceId)
                .orElseThrow(() -> new NotFoundException("Failure snapshot not found for instance: " + instanceId));
        return FailureSnapshotResponse.builder()
                .id(snapshot.getId())
                .instanceId(snapshot.getInstance().getId())
                .category(snapshot.getCategory())
                .description(snapshot.getDescription())
                .snapshot(snapshot.getSnapshot())
                .createdBy(snapshot.getCreatedBy())
                .createdAt(snapshot.getCreatedAt())
                .build();
    }
}
