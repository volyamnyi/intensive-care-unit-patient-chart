package com.superhumans.controller;

import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.dto.DepartmentPatientResponse;
import com.superhumans.dto.DepartmentStatsResponse;
import com.superhumans.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class DepartmentControllerTest {

    DepartmentService departmentService;

    @GetMapping
    public ResponseEntity<List<DepartmentPatientResponse>> getAll() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<DepartmentStatsResponse> getStats(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentStats(id));
    }

    @GetMapping("/{id}/patients")
    public ResponseEntity<List<DepartmentPatientResponse>> getPatients(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentPatients(id));
    }
}