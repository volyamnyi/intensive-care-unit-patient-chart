package com.superhumans.medicationsheet.controller;

import com.superhumans.exception.BusinessException;
import com.superhumans.exception.ErrorCode;
import com.superhumans.medicationsheet.dto.DrugInteractionImportReport;
import com.superhumans.medicationsheet.dto.PrescriptionInteractionsResponse;
import com.superhumans.medicationsheet.service.DrugInteractionImportService;
import com.superhumans.medicationsheet.service.DrugInteractionImportService.ImportError;
import com.superhumans.medicationsheet.service.DrugInteractionWarningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

/**
 * Drug-interaction warning endpoints (issue #304):
 * <ul>
 *   <li>{@code GET /api/prescriptions/{listId}/interactions} — server-computed
 *       warnings for the "Заплановано" set of a prescription list (read-only,
 *       never blocking);</li>
 *   <li>{@code POST /api/admin/drug-interactions/import} — full-sync import of
 *       the dataset, ADMINISTRATOR only, audited.</li>
 * </ul>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Drug Interactions", description = "Взаємодії ліків — попередження та імпорт бази (Form 003-4/о)")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DrugInteractionController {

    static final long MAX_IMPORT_BYTES = 20L * 1024 * 1024;

    DrugInteractionWarningService warningService;
    DrugInteractionImportService importService;

    @GetMapping("/api/prescriptions/{listId}/interactions")
    @PreAuthorize("@permissionService.has('PATIENT_VIEW')")
    @Operation(summary = "Compute interaction warnings", description =
            "Dangerous (medium/high/critical) interactions among planned medicines "
                    + "with overlapping planned periods. Read-only, non-blocking.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Warnings computed"),
            @ApiResponse(responseCode = "404", description = "Prescription list not found")
    })
    public PrescriptionInteractionsResponse getInteractions(
            @Parameter(description = "Prescription list UUID") @PathVariable UUID listId) {
        return warningService.computeWarnings(listId);
    }

    @PostMapping(value = "/api/admin/drug-interactions/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Import the interactions dataset", description =
            "Full sync: both tables are replaced by the uploaded JSON in a single "
                    + "transaction. ADMINISTRATOR only, audited.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Import report"),
            @ApiResponse(responseCode = "400", description = "Invalid file or JSON"),
            @ApiResponse(responseCode = "403", description = "Not an administrator"),
            @ApiResponse(responseCode = "422", description = "Dataset rejected (no valid rows)")
    })
    public DrugInteractionImportReport importDataset(
            @RequestParam("file") MultipartFile file, Authentication auth) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new com.superhumans.exception.BadRequestException("Файл обов'язковий");
        }
        if (file.getSize() > MAX_IMPORT_BYTES) {
            throw new com.superhumans.exception.BadRequestException("Файл більший за 20 МБ");
        }
        byte[] content = file.getBytes();
        Long adminId = (auth != null && auth.getCredentials() instanceof Long uid) ? uid : 0L;
        try {
            return importService.importDataset(content, adminId);
        } catch (ImportError e) {
            String details = e.getSkippedDetails().stream().limit(5).collect(Collectors.joining("; "));
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "Імпорт відхилено: 0 коректних рядків. Приклади: " + details);
        }
    }
}
