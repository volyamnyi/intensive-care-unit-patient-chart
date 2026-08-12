package com.superhumans.controller;

import com.superhumans.dto.DepartmentPatientResponse;
import com.superhumans.dto.DepartmentStatsResponse;
import com.superhumans.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/department")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepartmentController {

    DepartmentService departmentService;

    @GetMapping("/stats")
    public ResponseEntity<DepartmentStatsResponse> getStats(
            @RequestParam(required = false) UUID departmentId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(departmentService.getStats(departmentId));
    }

    @GetMapping("/patients")
    public ResponseEntity<List<DepartmentPatientResponse>> getPatients(
            @RequestParam(required = false) UUID departmentId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(departmentService.getPatients(departmentId));
    }
}
