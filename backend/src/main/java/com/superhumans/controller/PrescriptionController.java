package com.superhumans.controller;

import com.superhumans.dto.*;
import com.superhumans.entity.*;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.AllergyMisDTO;
import com.superhumans.mis.dto.MedicineMisDTO;
import com.superhumans.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
@Tag(name = "Prescriptions", description = "Prescription management - листок лікарських призначень (Form 003-15/о)")
public class PrescriptionController {

    private final PrescriptionListService listService;
    private final PrescriptionItemService itemService;
    private final PrescriptionExecutionService executionService;
    private final VitalSignService vitalSignService;
    private final MisService misService;

    @GetMapping
    @Operation(summary = "Get prescriptions by patient ID", description = "Retrieves all prescription lists for a specific patient")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved prescription lists"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public List<PrescriptionListResponse> getByPatient(
            @Parameter(description = "Patient ID from MIS") @RequestParam Long patientId) {
        return listService.getByPatient(patientId).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get prescription list by ID", description = "Retrieves a specific prescription list by its UUID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved prescription list"),
            @ApiResponse(responseCode = "404", description = "Prescription list not found")
    })
    public PrescriptionListResponse getById(
            @Parameter(description = "Prescription list UUID") @PathVariable UUID id) {
        return toResponse(listService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create prescription list", description = "Creates a new prescription list for a patient. Requires DOCTOR or HEAD_OF_DEPARTMENT role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Prescription list created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    public PrescriptionListResponse create(
            @Valid @RequestBody PrescriptionListCreateRequest req) {
        return toResponse(listService.create(Long.parseLong(req.getPatientId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete prescription list", description = "Soft-deletes a prescription list. Requires DOCTOR or HEAD_OF_DEPARTMENT role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Prescription list deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Prescription list not found")
    })
    public void delete(@PathVariable UUID id) {
        listService.delete(id);
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close prescription list", description = "Marks a prescription list as closed. Requires DOCTOR or HEAD_OF_DEPARTMENT role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Prescription list closed successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot close - prescription not in valid state")
    })
    public PrescriptionListResponse close(@PathVariable UUID id) {
        listService.close(id);
        return toResponse(listService.getById(id));
    }

    @GetMapping("/{listId}/items")
    @Operation(summary = "Get prescription items", description = "Retrieves all medicine items for a prescription list")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved items"),
            @ApiResponse(responseCode = "404", description = "Prescription list not found")
    })
    public List<PrescriptionItemResponse> getItems(@PathVariable UUID listId) {
        return itemService.getByList(listId).stream().map(this::toItemResponse).toList();
    }

    @PostMapping("/{listId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add medicine item", description = "Adds a new medicine item to the prescription list. Creates 21-day grid automatically.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Medicine item added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    public PrescriptionItemResponse addItem(
            @PathVariable UUID listId,
            @Valid @RequestBody PrescriptionItemAddRequest req) {
        return toItemResponse(itemService.addItem(listId, req.getMedicineName(), req.getMedicineMethod(), req.getRegime()));
    }

    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove medicine item", description = "Removes a medicine item from the prescription list")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Medicine item removed successfully"),
            @ApiResponse(responseCode = "404", description = "Medicine item not found")
    })
    public void removeItem(@PathVariable UUID itemId) {
        itemService.removeItem(itemId);
    }

    @PutMapping("/day-parts/{dayPartId}/plan")
    @Operation(summary = "Plan dose for day part", description = "Plans a specific dose for a day part. Requires DOCTOR or HEAD_OF_DEPARTMENT role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dose planned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid dose value")
    })
    public PrescriptionDayPartResponse planDose(
            @PathVariable UUID dayPartId,
            @Valid @RequestBody PrescriptionDoseRequest req) {
        UUID dummyId = UUID.randomUUID();
        return toPartResponse(itemService.planDose(dayPartId, req.getDose(), dummyId));
    }

    @PutMapping("/day-parts/{dayPartId}/complete")
    @Operation(summary = "Complete day part", description = "Marks a day part as completed. Requires NURSE or HEAD_OF_DEPARTMENT role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Day part completed successfully")
    })
    public PrescriptionDayPartResponse completeDose(@PathVariable UUID dayPartId) {
        return toPartResponse(itemService.markCompleted(dayPartId, UUID.randomUUID()));
    }

    @PostMapping("/day-parts/{dayPartId}/execute")
    @Operation(summary = "Execute dose", description = "Executes a dose for a day part. Requires NURSE or HEAD_OF_DEPARTMENT role. May require 2-person authentication for high-risk medicines.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dose executed successfully"),
            @ApiResponse(responseCode = "400", description = "Execution failed - invalid state or missing 2-person auth")
    })
    public void executeDose(
            @PathVariable UUID dayPartId,
            @Valid @RequestBody PrescriptionExecuteRequest req) {
        executionService.execute(dayPartId, UUID.randomUUID(), req.getActualDose(), req.isRequires2pAuth(), req.getSecondPersonId());
    }

    @GetMapping("/allergies")
    @Operation(summary = "Get patient allergies", description = "Retrieves allergy information from MIS for a specific patient")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved allergies")
    })
    public List<AllergyResponse> getAllergies(
            @Parameter(description = "Patient ID from MIS") @RequestParam Long patientId) {
        return misService.getPatientAllergies(patientId).stream()
                .map(this::toAllergyResponse)
                .toList();
    }

    @GetMapping("/medicine-catalog")
    @Operation(summary = "Search medicine catalog", description = "Searches the medicine catalog from MIS. Returns empty list if no keyword provided.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved medicine catalog")
    })
    public List<MedicineCatalogResponse> searchMedicineCatalog(
            @Parameter(description = "Search keyword (optional)") @RequestParam(required = false) String keyword) {
        return misService.searchMedicineCatalog(keyword == null ? "" : keyword).stream()
                .map(this::toMedicineResponse)
                .toList();
    }

    private PrescriptionListResponse toResponse(PrescriptionList l) {
        return PrescriptionListResponse.builder()
                .id(l.getId()).patientId(l.getPatientId())
                .hospitalizationId(l.getHospitalizationId()).departmentId(l.getDepartmentId())
                .documentName(l.getDocumentName()).status(l.getStatus()).editingUserId(l.getEditingUserId())
                .build();
    }

    private PrescriptionItemResponse toItemResponse(PrescriptionItem i) {
        return PrescriptionItemResponse.builder()
                .id(i.getId()).listId(i.getList().getId()).medicineName(i.getMedicineName())
                .medicineMethod(i.getMedicineMethod()).regime(i.getRegime())
                .status(i.getStatus()).sortOrder(i.getSortOrder())
                .build();
    }

    private PrescriptionDayPartResponse toPartResponse(PrescriptionDayPart p) {
        return PrescriptionDayPartResponse.builder()
                .id(p.getId()).dayId(p.getDay().getId()).period(p.getPeriod()).dose(p.getDose())
                .isPlanned(p.getIsPlanned()).isPlannedFinished(p.getIsPlannedFinished())
                .isCompleted(p.getIsCompleted()).isCompletedFinished(p.getIsCompletedFinished())
                .doctorName(p.getDoctorName()).nurseName(p.getNurseName())
                .build();
    }

    private AllergyResponse toAllergyResponse(AllergyMisDTO dto) {
        return new AllergyResponse(
                dto.getPatientId() + "-" + dto.getAllergenName(),
                dto.getPatientId(),
                dto.getAllergenName(),
                dto.getSourceDocumentId()
        );
    }

    private MedicineCatalogResponse toMedicineResponse(MedicineMisDTO dto) {
        return new MedicineCatalogResponse(
                dto.getId(),
                dto.getName(),
                dto.getCategoryRef(),
                dto.getPtgCode(),
                false
        );
    }
}
