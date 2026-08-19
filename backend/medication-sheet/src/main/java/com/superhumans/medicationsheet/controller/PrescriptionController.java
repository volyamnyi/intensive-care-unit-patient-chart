package com.superhumans.medicationsheet.controller;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import com.superhumans.medicationsheet.dto.*;
import com.superhumans.medicationsheet.mapper.*;
import com.superhumans.mis.MisService;
import com.superhumans.medicationsheet.service.PrescriptionExecutionService;
import com.superhumans.medicationsheet.service.PrescriptionItemService;
import com.superhumans.medicationsheet.service.PrescriptionListService;
import com.superhumans.medicationsheet.service.VitalSignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
@Tag(name = "Prescriptions", description = "Prescription management - листок лікарських призначень (Form 003-15/о)")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PrescriptionController {

    PrescriptionListService listService;
    PrescriptionItemService itemService;
    PrescriptionExecutionService executionService;
    VitalSignService vitalSignService;
    MisService misService;
    PrescriptionListMapper prescriptionListMapper;
    PrescriptionItemMapper prescriptionItemMapper;
    PrescriptionDayPartMapper prescriptionDayPartMapper;
    AllergyMapper allergyMapper;
    MedicineCatalogMapper medicineCatalogMapper;

    @GetMapping
    @PreAuthorize("@permissionService.has('PATIENT_VIEW')")
    @Operation(summary = "Get prescriptions by patient ID", description = "Retrieves all prescription lists for a specific patient")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved prescription lists"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public List<PrescriptionListResponse> getByPatient(
            @Parameter(description = "Patient ID from MIS") @RequestParam Long patientId) {
        return listService.getByPatient(patientId).stream().map(prescriptionListMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.has('PATIENT_VIEW')")
    @Operation(summary = "Get prescription list by ID", description = "Retrieves a specific prescription list by its UUID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved prescription list"),
            @ApiResponse(responseCode = "404", description = "Prescription list not found")
    })
    public PrescriptionListResponse getById(
            @Parameter(description = "Prescription list UUID") @PathVariable UUID id) {
        return prescriptionListMapper.toResponse(listService.getById(id));
    }

    @PreAuthorize("@permissionService.has('PRESCRIPTION_LIST_CREATE')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create prescription list", description = "Creates a new prescription list for a patient. Requires PRESCRIPTION_LIST_CREATE permission (DOCTOR by default).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Prescription list created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    public PrescriptionListResponse create(
            @Valid @RequestBody PrescriptionListCreateRequest req) {
        return prescriptionListMapper.toResponse(listService.create(Long.parseLong(req.getPatientId())));
    }

    @PreAuthorize("@permissionService.has('PRESCRIPTION_CREATE')")
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

    @PreAuthorize("@permissionService.has('PRESCRIPTION_CREATE')")
    @PostMapping("/{id}/close")
    @Operation(summary = "Close prescription list", description = "Marks a prescription list as closed. Requires DOCTOR or HEAD_OF_DEPARTMENT role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Prescription list closed successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot close - prescription not in valid state")
    })
    public PrescriptionListResponse close(@PathVariable UUID id) {
        listService.close(id);
        return prescriptionListMapper.toResponse(listService.getById(id));
    }

    @GetMapping("/{listId}/items")
    @PreAuthorize("@permissionService.has('PATIENT_VIEW')")
    @Operation(summary = "Get prescription items", description = "Retrieves all medicine items for a prescription list")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved items"),
            @ApiResponse(responseCode = "404", description = "Prescription list not found")
    })
    public List<PrescriptionItemResponse> getItems(@PathVariable UUID listId) {
        return itemService.getByList(listId).stream().map(prescriptionItemMapper::toResponse).toList();
    }

    @PreAuthorize("@permissionService.has('PRESCRIPTION_CREATE')")
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
        return prescriptionItemMapper.toResponse(itemService.addItem(listId, req.getMedicineName(), req.getMedicineMethod(), req.getRegime()));
    }

    @PreAuthorize("@permissionService.has('PRESCRIPTION_CREATE')")
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

    @PreAuthorize("@permissionService.has('PRESCRIPTION_CREATE')")
    @PutMapping("/day-parts/{dayPartId}/plan")
    @Operation(summary = "Plan dose for day part", description = "Plans a specific dose for a day part. Requires DOCTOR or HEAD_OF_DEPARTMENT role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dose planned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid dose value")
    })
    public PrescriptionDayPartResponse planDose(
            @PathVariable UUID dayPartId,
            @Valid @RequestBody PrescriptionDoseRequest req) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = (Long) auth.getCredentials();
        UUID currentUserUuid = UUID.nameUUIDFromBytes(currentUserId.toString().getBytes());
        return prescriptionDayPartMapper.toResponse(itemService.planDose(dayPartId, req.getDose(), currentUserUuid));
    }

    @PreAuthorize("@permissionService.has('PRESCRIPTION_EXECUTE')")
    @PutMapping("/day-parts/{dayPartId}/complete")
    @Operation(summary = "Complete day part", description = "Marks a day part as completed. Requires NURSE or HEAD_OF_DEPARTMENT role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Day part completed successfully")
    })
    public PrescriptionDayPartResponse completeDose(@PathVariable UUID dayPartId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = (Long) auth.getCredentials();
        UUID currentUserUuid = UUID.nameUUIDFromBytes(currentUserId.toString().getBytes());
        return prescriptionDayPartMapper.toResponse(itemService.markCompleted(dayPartId, currentUserUuid));
    }

    @PreAuthorize("@permissionService.has('PRESCRIPTION_CREATE')")
    @PutMapping("/day-parts/{dayPartId}/cancel")
    @Operation(summary = "Cancel planned dose", description = "Marks a planned day part as cancelled (isPlannedFinished). Requires DOCTOR or HEAD_OF_DEPARTMENT role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Planned dose cancelled successfully")
    })
    public PrescriptionDayPartResponse cancelDose(@PathVariable UUID dayPartId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = (Long) auth.getCredentials();
        UUID currentUserUuid = UUID.nameUUIDFromBytes(currentUserId.toString().getBytes());
        return prescriptionDayPartMapper.toResponse(itemService.markPlannedFinished(dayPartId, currentUserUuid));
    }

    @PreAuthorize("@permissionService.has('PRESCRIPTION_EXECUTE')")
    @PostMapping("/day-parts/{dayPartId}/execute")
    @Operation(summary = "Execute dose", description = "Executes a dose for a day part. Requires NURSE or HEAD_OF_DEPARTMENT role. Requires 2-person authentication with a different nurse's credentials.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dose executed successfully"),
            @ApiResponse(responseCode = "400", description = "Execution failed - invalid second-person credentials or same person")
    })
    public void executeDose(
            @PathVariable UUID dayPartId,
            @Valid @RequestBody PrescriptionExecuteRequest req) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = (Long) auth.getCredentials();
        String currentUserLogin = (String) auth.getPrincipal();
        executionService.execute(dayPartId, currentUserId, currentUserLogin,
                req.getActualDose(), req.getSecondPersonLogin(), req.getSecondPersonPassword(),
                req.isRequires2pAuth());
    }

    @GetMapping("/allergies")
    @PreAuthorize("@permissionService.has('PATIENT_VIEW')")
    @Operation(summary = "Get patient allergies", description = "Retrieves allergy information from MIS for a specific patient")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved allergies")
    })
    public List<AllergyResponse> getAllergies(
            @Parameter(description = "Patient ID from MIS") @RequestParam Long patientId) {
        return misService.getPatientAllergies(patientId).stream()
                .map(allergyMapper::toResponse)
                .toList();
    }

    @GetMapping("/medicine-catalog")
    @PreAuthorize("@permissionService.has('PATIENT_VIEW')")
    @Operation(summary = "Search medicine catalog", description = "Searches the medicine catalog from MIS. Returns empty list if no keyword provided.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved medicine catalog")
    })
    public List<MedicineCatalogResponse> searchMedicineCatalog(
            @Parameter(description = "Search keyword (optional)") @RequestParam(required = false) String keyword) {
        return misService.searchMedicineCatalog(keyword == null ? "" : keyword).stream()
                .map(medicineCatalogMapper::toResponse)
                .toList();
    }
}
