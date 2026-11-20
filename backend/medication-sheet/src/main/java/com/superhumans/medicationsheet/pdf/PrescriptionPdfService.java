package com.superhumans.medicationsheet.pdf;

import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.exception.NotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PrescriptionPdfService {
    private final PrescriptionPdfDataLoader loader;
    private final PrescriptionPdfPagePlanner planner;
    private final PrescriptionPdfRenderer renderer;
    private final PrescriptionPdfAuditService audit;

    public record PdfBatchInfo(int pages, String fileName) { }
    public record PdfBatchDownload(PdfBatchInfo info, byte[] content) { }

    public PdfBatchInfo info(UUID id) {
        return audited(id, ActionType.PDF_INFO, () -> {
            var pages = plan(id, null, null);
            audit.recordInfo(id, pages.size());
            return new PdfBatchInfo(pages.size(), zipFileName(id));
        });
    }

    public PdfBatchDownload document(UUID id, Long userId, LocalDate from, LocalDate to) {
        return audited(id, ActionType.GENERATE, () -> {
            var pages = plan(id, from, to);
            byte[] pdf = pages.isEmpty() ? new byte[0] : renderer.render(pages);
            audit.recordGenerate(id, userId, pages.size());
            return new PdfBatchDownload(new PdfBatchInfo(pages.size(), "prescription-" + id + ".pdf"), pdf);
        });
    }

    public byte[] generateZip(UUID id, Long userId) {
        return generateZipBatch(id, userId).content();
    }

    public PdfBatchDownload generateZipBatch(UUID id, Long userId) {
        return audited(id, ActionType.GENERATE, () -> {
            var pages = plan(id, null, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(out)) {
                for (var page : pages) {
                    byte[] bytes = renderer.render(List.of(page));
                    ZipEntry entry = new ZipEntry(pageFileName(id, page.pageIndex()));
                    CRC32 crc = new CRC32();
                    crc.update(bytes);
                    entry.setMethod(ZipEntry.STORED);
                    entry.setSize(bytes.length);
                    entry.setCompressedSize(bytes.length);
                    entry.setCrc(crc.getValue());
                    zip.putNextEntry(entry);
                    zip.write(bytes);
                    zip.closeEntry();
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to generate prescription ZIP", e);
            }
            audit.recordGenerate(id, userId, pages.size());
            return new PdfBatchDownload(new PdfBatchInfo(pages.size(), zipFileName(id)), out.toByteArray());
        });
    }

    public byte[] generatePage(UUID id, int index, Long userId) {
        return audited(id, ActionType.DOWNLOAD, () -> {
            var pages = plan(id, null, null);
            if (index < 0 || index >= pages.size()) {
                throw new NotFoundException("Сторінку PDF не знайдено: " + index);
            }
            byte[] pdf = renderer.render(List.of(pages.get(index)));
            audit.recordPageDownload(id, userId);
            return pdf;
        });
    }

    private List<PrescriptionPdfPagePlan> plan(UUID id, LocalDate from, LocalDate to) {
        var data = loader.load(id);
        return planner.plan(data.snapshot(), data.header(), data.logins(), from, to);
    }

    private <T> T audited(UUID id, ActionType type, Supplier<T> work) {
        try {
            return work.get();
        } catch (RuntimeException failure) {
            audit.recordFailure(id, type);
            throw failure;
        }
    }

    private static String zipFileName(UUID id) {
        return "prescription-" + id + ".zip";
    }

    public static String pageFileName(UUID id, int index) {
        return String.format(Locale.ROOT, "prescription-%s-p%02d.pdf", id, index + 1);
    }
}
