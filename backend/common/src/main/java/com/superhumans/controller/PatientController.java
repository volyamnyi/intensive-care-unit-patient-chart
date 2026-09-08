package com.superhumans.controller;

import com.superhumans.mis.MisService;
import com.superhumans.mis.PatientModuleFilter;
import com.superhumans.mis.dto.PatientDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PatientController {

    MisService misService;

    /**
     * Patient search. Without {@code module} the contract is unchanged
     * (MIS search by query). With {@code module=medication} the result is
     * narrowed to the module roster (departments 19/37, #260) — the query
     * still applies first, so both orders commute to the same list.
     */
    @GetMapping
    public ResponseEntity<List<PatientDTO>> searchPatients(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String module) {
        List<PatientDTO> result = misService.searchPatients(query);
        if (module != null) {
            result = PatientModuleFilter.filter(module, result);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientDTO> getPatient(@PathVariable Long id) {
        return misService.getPatient(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
