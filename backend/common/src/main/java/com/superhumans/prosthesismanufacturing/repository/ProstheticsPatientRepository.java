package com.superhumans.prosthesismanufacturing.repository;


import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProstheticsPatientRepository extends JpaRepository<ProstheticsPatient, String> {
    List<ProstheticsPatient> findByPibContainingIgnoreCase(String query);
    List<ProstheticsPatient> findByPibIgnoreCase(String pib);
}
