package com.superhumans.mis;

import com.superhumans.mis.dto.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MisService {

    Optional<PatientDTO> getPatient(Long patientId);

    Optional<HospitalizationDTO> getHospitalization(UUID hospitalizationId);

    Optional<UserMisDTO> getUser(Long userId);

    List<UserMisDTO> getDepartmentUsers(Long departmentId);

    List<DepartmentDTO> getDepartments();

    List<DictionaryItemDTO> getDictionary(String dictionaryName);

    List<PatientDTO> searchPatients(String query);

    boolean sendPdf(UUID clinicalDayId, byte[] pdfContent, String fileName, int version);
}
