package com.superhumans.service;

import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.DataClass;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import com.superhumans.audit.AuditActorResolver;
import com.superhumans.audit.AuditChanges;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.DomainAuditEmitter;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Keeps ICU PDF audit persistence short and separate from rendering/MIS I/O. */
@Service
@RequiredArgsConstructor
public class IcuPdfAuditService {

    private final AuditService auditService;
    private final DomainAuditEmitter auditEmitter;

    @Transactional
    public void recordGenerated(UUID clinicalDayId, UUID generatedPdfId, Long userId, int pageCount) {
        auditService.logAction("GeneratedPdf", clinicalDayId, "GENERATE", userId);
        auditEmitter.emit("icu", () -> AuditEvent.builder()
                .actor(AuditActorResolver.fromCurrentContextOrSystem("icu-pdf-job"))
                .eventClass(EventClass.BUSINESS)
                .module("icu")
                .functionalArea("pdf")
                .action("icu.pdf.generate")
                .actionType(ActionType.GENERATE)
                .target(new AuditEvent.AuditTarget("GeneratedPdf", generatedPdfId.toString(), null))
                .relatedEntities(List.of(new AuditEvent.AuditTarget(
                        "ClinicalDay", clinicalDayId.toString(), null)))
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .changes(List.of(AuditChanges.fieldChanged("fileVersion", DataClass.IDENTIFIER)))
                .affectedRecords(pageCount)
                .source(AuditEvent.AuditSource.API)
                .build());
    }
}
