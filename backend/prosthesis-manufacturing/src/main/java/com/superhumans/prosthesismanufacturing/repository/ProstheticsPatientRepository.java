package com.superhumans.prosthesismanufacturing.repository;


import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProstheticsPatientRepository extends JpaRepository<ProstheticsPatient, UUID> {
    List<ProstheticsPatient> findByPibContainingIgnoreCase(String query);
    List<ProstheticsPatient> findByPibIgnoreCase(String pib);
}
