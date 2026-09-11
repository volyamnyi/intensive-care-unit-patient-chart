package com.superhumans.medicationsheet.pdf;

import com.superhumans.entity.core.User;
import com.superhumans.exception.NotFoundException;
import com.superhumans.mis.dto.PatientDTO;
import com.superhumans.repository.core.UserRepository;
import com.superhumans.service.AuditService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates Phase 17 PDF generation for one {@code PrescriptionList}:
 * consistent snapshot → resolved usernames → deterministic page plans →
 * one iText page per plan → ZIP batch (one PDF file per form sheet).
 * Stateless: nothing is persisted, nothing is sent to MIS.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PrescriptionPdfService {

    PrescriptionPdfDataLoader dataLoader;
    PrescriptionPdfUsernameResolver usernameResolver;
    PrescriptionPdfPagePlanner pagePlanner;
    PrescriptionPdfRenderer renderer;
    UserRepository userRepository;
    AuditService auditService;

    /** Batch metadata for the API/frontend contract (no PII). */
    public record PdfBatchInfo(int pages, String fileName) {
    }

    /** One rendered PDF page with its deterministic file name (no PII). */
    public record PdfPageFile(String fileName, byte[] content) {
    }

    @Transactional(readOnly = true)
    public PdfBatchInfo info(UUID listId) {
        List<PrescriptionPdfPagePlan> pages = plan(listId);
        return new PdfBatchInfo(pages.size(), zipFileName(listId));
    }

    @Transactional(readOnly = true)
    public byte[] generateZip(UUID listId, Long userId) {
        List<PdfPageFile> files = renderAll(listId);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // STORED (uncompressed) entries with upfront sizes: PDFs are already
        // compressed, and explicit headers keep the archive readable by
        // minimal parsers (no data descriptors).
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (PdfPageFile file : files) {
                ZipEntry entry = new ZipEntry(file.fileName());
                entry.setMethod(ZipEntry.STORED);
                entry.setSize(file.content().length);
                entry.setCompressedSize(file.content().length);
                entry.setCrc(crc32(file.content()));
                zip.putNextEntry(entry);
                zip.write(file.content());
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build prescription PDF batch for " + listId, e);
        }
        auditService.logAction("PrescriptionPdf", listId, "GENERATE", userId);
        log.info("Prescription PDF batch generated: listId={}, pages={}", listId, files.size());
        return out.toByteArray();
    }

    private static long crc32(byte[] content) {
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(content);
        return crc.getValue();
    }

    @Transactional(readOnly = true)
    public byte[] generatePage(UUID listId, int pageIndex, Long userId) {
        List<PrescriptionPdfPagePlan> pages = plan(listId);
        if (pageIndex < 0 || pageIndex >= pages.size()) {
            throw new NotFoundException("Сторінку PDF не знайдено: " + pageIndex);
        }
        byte[] content = renderer.render(pages.get(pageIndex));
        auditService.logAction("PrescriptionPdf", listId, "GENERATE_PAGE", userId);
        return content;
    }

    private List<PdfPageFile> renderAll(UUID listId) {
        List<PrescriptionPdfPagePlan> pages = plan(listId);
        List<PdfPageFile> files = new ArrayList<>();
        for (PrescriptionPdfPagePlan page : pages) {
            files.add(new PdfPageFile(pageFileName(listId, page.pageIndex()), renderer.render(page)));
        }
        return files;
    }

    private List<PrescriptionPdfPagePlan> plan(UUID listId) {
        PrescriptionPdfDataLoader.LoadedSnapshot loaded = dataLoader.load(listId);
        PrescriptionPdfSnapshot snapshot = loaded.snapshot();

        List<User> users = userRepository.findAll();
        Map<UUID, String> loginIndex = usernameResolver.buildLoginIndex(users);
        String doctorUsername = usernameResolver.resolveAttendingDoctor(users);

        String cardNumber = snapshot.hospitalizationId() == null ? "" : snapshot.hospitalizationId().toString();
        String patientName = loaded.patient().map(PatientDTO::getFullName).orElse("");
        String room = loaded.patient().map(PatientDTO::getRoom).orElse("");
        PrescriptionPdfPagePlanner.HeaderData header = new PrescriptionPdfPagePlanner.HeaderData(
                loaded.institutionName() == null ? "" : loaded.institutionName(),
                loaded.edrpou() == null ? "" : loaded.edrpou(),
                cardNumber,
                patientName == null ? "" : patientName,
                room == null ? "" : room);

        List<PrescriptionPdfPagePlan> pages = pagePlanner.plan(snapshot, header, loginIndex, doctorUsername);
        if (pages.isEmpty()) {
            throw new IllegalStateException("PDF planning produced no pages for " + listId);
        }
        return pages;
    }

    static String zipFileName(UUID listId) {
        return "prescription-" + listId + ".zip";
    }

    static String pageFileName(UUID listId, int pageIndex) {
        return "prescription-" + listId + "-p" + String.format("%02d", pageIndex + 1) + ".pdf";
    }
}
