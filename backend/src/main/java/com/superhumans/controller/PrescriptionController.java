package com.superhumans.controller;

import com.superhumans.dto.*;
import com.superhumans.entity.*;
import com.superhumans.service.*;
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
public class PrescriptionController {

    private final PrescriptionListService listService;
    private final PrescriptionItemService itemService;
    private final PrescriptionExecutionService executionService;
    private final VitalSignService vitalSignService;

    @GetMapping
    public List<PrescriptionListResponse> getByPatient(@RequestParam Long patientId) {
        return listService.getByPatient(patientId).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public PrescriptionListResponse getById(@PathVariable UUID id) {
        return toResponse(listService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrescriptionListResponse create(@Valid @RequestBody PrescriptionListCreateRequest req) {
        return toResponse(listService.create(Long.parseLong(req.getPatientId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        listService.delete(id);
    }

    @PostMapping("/{id}/close")
    public PrescriptionListResponse close(@PathVariable UUID id) {
        listService.close(id);
        return toResponse(listService.getById(id));
    }

    @GetMapping("/{listId}/items")
    public List<PrescriptionItemResponse> getItems(@PathVariable UUID listId) {
        return itemService.getByList(listId).stream().map(this::toItemResponse).toList();
    }

    @PostMapping("/{listId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public PrescriptionItemResponse addItem(@PathVariable UUID listId, @Valid @RequestBody PrescriptionItemAddRequest req) {
        return toItemResponse(itemService.addItem(listId, req.getMedicineName(), req.getMedicineMethod(), req.getRegime()));
    }

    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(@PathVariable UUID itemId) {
        itemService.removeItem(itemId);
    }

    @PutMapping("/day-parts/{dayPartId}/plan")
    public PrescriptionDayPartResponse planDose(@PathVariable UUID dayPartId, @Valid @RequestBody PrescriptionDoseRequest req) {
        UUID dummyId = UUID.randomUUID();
        return toPartResponse(itemService.planDose(dayPartId, req.getDose(), dummyId));
    }

    @PutMapping("/day-parts/{dayPartId}/complete")
    public PrescriptionDayPartResponse completeDose(@PathVariable UUID dayPartId) {
        return toPartResponse(itemService.markCompleted(dayPartId, UUID.randomUUID()));
    }

    @PostMapping("/day-parts/{dayPartId}/execute")
    public void executeDose(@PathVariable UUID dayPartId, @Valid @RequestBody PrescriptionExecuteRequest req) {
        executionService.execute(dayPartId, UUID.randomUUID(), req.getActualDose(), req.isRequires2pAuth(), req.getSecondPersonId());
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
}
