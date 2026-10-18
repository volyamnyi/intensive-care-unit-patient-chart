package com.superhumans.medicationsheet.pdf;

import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import com.superhumans.audit.AuditActorResolver;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.DomainAuditEmitter;
import com.superhumans.service.AuditService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Keeps PDF audit writes short and separate from MIS access and iText rendering. */
@Service
@RequiredArgsConstructor
public class PrescriptionPdfAuditService {

    private final AuditService auditService;
    private final DomainAuditEmitter auditEmitter;

    @Transactional
    public void recordInfo(UUID listId, int pageCount) {
        auditEmitter.emit("medication", () -> AuditEvent.builder()
                .actor(AuditActorResolver.fromCurrentContext())
                .eventClass(EventClass.USER_ACTIVITY)
                .module("medication")
                .functionalArea("pdf")
                .action("medication.pdf.info")
                .actionType(ActionType.PDF_INFO)
                .target(new AuditEvent.AuditTarget("PrescriptionList", listId.toString(), null))
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .affectedRecords(pageCount)
                .source(AuditEvent.AuditSource.API)
                .build());
    }

    @Transactional
    public void recordGenerate(UUID listId, Long userId, int pageCount) {
        auditService.logAction("PrescriptionPdf", listId, "GENERATE", userId);
        auditEmitter.emit("medication", () -> AuditEvent.builder()
                .actor(AuditActorResolver.fromCurrentContext())
                .eventClass(EventClass.BUSINESS)
                .module("medication")
                .functionalArea("pdf")
                .action("medication.pdf.generate")
                .actionType(ActionType.GENERATE)
                .target(new AuditEvent.AuditTarget("PrescriptionList", listId.toString(), null))
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .affectedRecords(pageCount)
                .source(AuditEvent.AuditSource.API)
                .build());
    }

    @Transactional
    public void recordPageDownload(UUID listId, Long userId) {
        auditService.logAction("PrescriptionPdf", listId, "GENERATE_PAGE", userId);
        auditEmitter.emit("medication", () -> AuditEvent.builder()
                .actor(AuditActorResolver.fromCurrentContext())
                .eventClass(EventClass.USER_ACTIVITY)
                .module("medication")
                .functionalArea("pdf")
                .action("medication.pdf.download")
                .actionType(ActionType.DOWNLOAD)
                .target(new AuditEvent.AuditTarget("PrescriptionList", listId.toString(), null))
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .source(AuditEvent.AuditSource.API)
                .build());
    }
}
