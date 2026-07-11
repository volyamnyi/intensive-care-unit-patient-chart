package com.superhumans.mis;

import com.superhumans.mis.dto.DictionaryItemDTO;
import com.superhumans.mis.dto.DocumentDTO;
import com.superhumans.mis.dto.PatientDTO;
import com.superhumans.mis.dto.UserMISDTO;

import java.time.LocalDate;
import java.util.List;

public interface MISService {
    List<PatientDTO> searchPatients(String name, String phone, String externalId);
    PatientDTO getPatientInfo(Integer patientId, String documentSequenceNumber);
    UserMISDTO getUserDetails(String login, String specialityCode);
    List<DocumentDTO> getPatientDocuments(Integer patientId, LocalDate start, LocalDate end);
    List<DictionaryItemDTO> getDocumentApproveStatuses();
}
