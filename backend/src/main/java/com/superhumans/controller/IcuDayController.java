package com.superhumans.controller;

import com.superhumans.dto.VitalSignsRequest;
import com.superhumans.dto.FluidIntakeRequest;
import com.superhumans.dto.FluidOutputRequest;
import com.superhumans.dto.NoteRequest;
import com.superhumans.dto.CareMeasureRequest;
import com.superhumans.entity.*;
import com.superhumans.repository.FluidIntakeRepository;
import com.superhumans.repository.FluidOutputRepository;
import com.superhumans.repository.IcuDayRepository;
import com.superhumans.repository.UserRepository;
import com.superhumans.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/icu-days")
@RequiredArgsConstructor
public class IcuDayController {

    private final IcuDayService icuDayService;
    private final VitalSignService vitalSignService;
    private final FluidBalanceService fluidBalanceService;
    private final ScaleService scaleService;
    private final PdfGeneratorService pdfGeneratorService;
    private final IcuCardService icuCardService;
    private final UserRepository userRepository;
    private final FluidIntakeRepository fluidIntakeRepository;
    private final FluidOutputRepository fluidOutputRepository;
    private final IcuDayRepository icuDayRepository;
    private final ClinicalNoteService clinicalNoteService;
    private final CareMeasureService careMeasureService;

    @GetMapping("/by-card/{cardId}")
    public ResponseEntity<List<IcuDay>> getDaysByCard(@PathVariable Long cardId) {
        return ResponseEntity.ok(icuDayService.getDaysByCard(cardId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IcuDay> getDay(@PathVariable Long id) {
        return ResponseEntity.ok(icuDayService.getDay(id));
    }

    // Vital signs
    @PutMapping("/{dayId}/vitals/{hour}")
    public ResponseEntity<HourlyVital> saveVitals(
            @PathVariable Long dayId,
            @PathVariable Integer hour,
            @RequestBody VitalSignsRequest req) {
        return ResponseEntity.ok(vitalSignService.saveVitals(dayId, hour, req));
    }

    @GetMapping("/{dayId}/vitals")
    public ResponseEntity<List<HourlyVital>> getVitals(@PathVariable Long dayId) {
        return ResponseEntity.ok(vitalSignService.getVitalsByDay(dayId));
    }

    // Fluid intake
    @PostMapping("/{dayId}/intake/{hour}")
    public ResponseEntity<FluidIntake> addIntake(
            @PathVariable Long dayId,
            @PathVariable Integer hour,
            @RequestBody FluidIntakeRequest req) {
        FluidIntake intake = FluidIntake.builder()
                .icuDay(IcuDay.builder().id(dayId).build())
                .hour(hour)
                .medicationName(req.getMedicationName())
                .volumeOrdered(req.getVolumeOrdered())
                .volumeActual(req.getVolumeActual())
                .prescriptionId(req.getPrescriptionId())
                .status(ExecutionStatus.DONE)
                .build();
        return ResponseEntity.ok(fluidIntakeRepository.save(intake));
    }

    // Fluid output
    @PostMapping("/{dayId}/output/{hour}")
    public ResponseEntity<FluidOutput> addOutput(
            @PathVariable Long dayId,
            @PathVariable Integer hour,
            @RequestBody FluidOutputRequest req) {
        FluidOutput output = FluidOutput.builder()
                .icuDay(IcuDay.builder().id(dayId).build())
                .hour(hour)
                .type(OutputType.valueOf(req.getType()))
                .volume(req.getVolume())
                .isPresent(req.getIsPresent())
                .build();
        return ResponseEntity.ok(fluidOutputRepository.save(output));
    }

    // Fluid balance
    @GetMapping("/{dayId}/balance")
    public ResponseEntity<?> getBalance(@PathVariable Long dayId) {
        return ResponseEntity.ok(fluidBalanceService.getBalance(dayId));
    }

    // Scales
    @PostMapping("/{dayId}/scales")
    public ResponseEntity<ScaleAssessment> saveScale(
            @PathVariable Long dayId,
            @RequestBody com.superhumans.dto.ScaleRequest req,
            Authentication auth) {
        return ResponseEntity.ok(scaleService.saveScale(dayId, req, auth.getName()));
    }

    @GetMapping("/{dayId}/scales")
    public ResponseEntity<List<ScaleAssessment>> getScales(@PathVariable Long dayId) {
        return ResponseEntity.ok(scaleService.getScalesByDay(dayId));
    }

    // APACHE II / SOFA update
    @PutMapping("/{dayId}/apache-sofa")
    public ResponseEntity<IcuDay> updateApacheSofa(
            @PathVariable Long dayId,
            @RequestBody java.util.Map<String, Integer> body) {
        IcuDay day = icuDayService.getDay(dayId);
        if (body.containsKey("apacheIi")) day.setApacheIi(body.get("apacheIi"));
        if (body.containsKey("sofa")) day.setSofa(body.get("sofa"));
        return ResponseEntity.ok(icuDayRepository.save(day));
    }

    // Sign-off
    @PostMapping("/{dayId}/sign-off")
    public ResponseEntity<IcuDay> signOff(@PathVariable Long dayId, Authentication auth) {
        User user = userRepository.findByLogin(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(icuDayService.signOff(dayId, user.getId(), auth.getName()));
    }

    // Clinical notes
    @PostMapping("/{dayId}/notes")
    public ResponseEntity<ClinicalNote> addNote(
            @PathVariable Long dayId,
            @RequestBody NoteRequest req,
            Authentication auth) {
        return ResponseEntity.ok(clinicalNoteService.addNote(dayId, req.getContent(), req.getNoteType(), auth.getName()));
    }

    @GetMapping("/{dayId}/notes")
    public ResponseEntity<List<ClinicalNote>> getNotes(@PathVariable Long dayId) {
        return ResponseEntity.ok(clinicalNoteService.getNotesByDay(dayId));
    }

    // Care measures
    @PostMapping("/{dayId}/care-measures")
    public ResponseEntity<CareMeasure> addCareMeasure(
            @PathVariable Long dayId,
            @RequestBody CareMeasureRequest req,
            Authentication auth) {
        return ResponseEntity.ok(careMeasureService.saveCareMeasure(dayId, req.getHour(), req.getProcedure(), req.getPerformed(), auth.getName()));
    }

    @GetMapping("/{dayId}/care-measures")
    public ResponseEntity<List<CareMeasure>> getCareMeasures(@PathVariable Long dayId) {
        return ResponseEntity.ok(careMeasureService.getCareMeasuresByDay(dayId));
    }

    // PDF
    @GetMapping("/{dayId}/pdf")
    public ResponseEntity<byte[]> generatePdf(@PathVariable Long dayId) {
        IcuDay day = icuDayService.getDay(dayId);
        IcuCard card = day.getIcuCard();
        byte[] pdf = pdfGeneratorService.generateDayPdf(day, card);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=icu-day-" + dayId + ".pdf")
                .body(pdf);
    }
}
