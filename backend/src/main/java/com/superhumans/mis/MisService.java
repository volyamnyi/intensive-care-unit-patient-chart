package com.superhumans.mis;

import com.superhumans.mis.dto.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MisService {

    Optional<PatientDTO> getPatient(UUID patientId);

    Optional<HospitalizationDTO> getHospitalization(UUID hospitalizationId);

    Optional<UserMisDTO> getUser(UUID userId);

    List<UserMisDTO> getDepartmentUsers(UUID departmentId);

    List<DepartmentDTO> getDepartments();

    List<DictionaryItemDTO> getDictionary(String dictionaryName);

    List<PatientDTO> searchPatients(String query);
}
